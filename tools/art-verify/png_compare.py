"""Dependency-free PNG RGBA/RGB decoder and screenshot comparator."""

from __future__ import annotations

import binascii
import struct
import zlib
from dataclasses import dataclass
from pathlib import Path

PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
Crop = tuple[int, int, int, int]


@dataclass(frozen=True)
class PngImage:
    width: int
    height: int
    pixels: bytes  # RGBA, 8 bits per channel, row-major


@dataclass(frozen=True)
class Comparison:
    width: int
    height: int
    differing_pixels: int
    differing_ratio: float
    max_error: int
    same_size: bool = True


def _paeth(a: int, b: int, c: int) -> int:
    estimate = a + b - c
    pa, pb, pc = abs(estimate - a), abs(estimate - b), abs(estimate - c)
    return a if pa <= pb and pa <= pc else (b if pb <= pc else c)


def read_png(path: str | Path) -> PngImage:
    data = Path(path).read_bytes()
    if not data.startswith(PNG_SIGNATURE):
        raise ValueError("not a PNG file")

    pos = len(PNG_SIGNATURE)
    idat = bytearray()
    width = height = color_type = bit_depth = interlace = None
    transparent_key: tuple[int, int, int] | None = None
    while pos < len(data):
        if pos + 12 > len(data):
            raise ValueError("truncated PNG chunk")
        length = struct.unpack_from(">I", data, pos)[0]
        kind = data[pos + 4 : pos + 8]
        end = pos + 12 + length
        if end > len(data):
            raise ValueError("truncated PNG chunk data")
        payload = data[pos + 8 : pos + 8 + length]
        crc = struct.unpack_from(">I", data, pos + 8 + length)[0]
        if binascii.crc32(kind + payload) & 0xFFFFFFFF != crc:
            raise ValueError("PNG chunk CRC mismatch")
        if kind == b"IHDR":
            if length != 13:
                raise ValueError("invalid PNG IHDR")
            width, height, bit_depth, color_type, _, _, interlace = struct.unpack(
                ">IIBBBBB", payload
            )
        elif kind == b"IDAT":
            idat.extend(payload)
        elif kind == b"tRNS":
            if color_type == 2:
                if length != 6:
                    raise ValueError("invalid RGB tRNS chunk")
                transparent_key = struct.unpack(">HHH", payload)
            elif color_type == 6:
                raise ValueError("tRNS is not allowed for RGBA PNGs")
            else:
                raise ValueError("unsupported PNG tRNS color type")
        elif kind == b"IEND":
            break
        pos = end

    if width is None or height is None:
        raise ValueError("PNG is missing IHDR")
    if bit_depth != 8 or color_type not in (2, 6) or interlace != 0:
        raise ValueError("only non-interlaced 8-bit RGB/RGBA PNGs are supported")
    channels = 3 if color_type == 2 else 4
    row_bytes = width * channels
    raw = zlib.decompress(bytes(idat))
    expected = height * (row_bytes + 1)
    if len(raw) != expected:
        raise ValueError("PNG scanline data has an unexpected size")

    rows: list[bytes] = []
    offset = 0
    previous = bytearray(row_bytes)
    for _ in range(height):
        filter_type = raw[offset]
        encoded = raw[offset + 1 : offset + 1 + row_bytes]
        offset += row_bytes + 1
        row = bytearray(encoded)
        for i in range(row_bytes):
            left = row[i - channels] if i >= channels else 0
            up = previous[i]
            upper_left = previous[i - channels] if i >= channels else 0
            if filter_type == 1:
                row[i] = (row[i] + left) & 0xFF
            elif filter_type == 2:
                row[i] = (row[i] + up) & 0xFF
            elif filter_type == 3:
                row[i] = (row[i] + ((left + up) // 2)) & 0xFF
            elif filter_type == 4:
                row[i] = (row[i] + _paeth(left, up, upper_left)) & 0xFF
            elif filter_type != 0:
                raise ValueError("unsupported PNG filter type")
        rows.append(bytes(row))
        previous = row

    if channels == 4:
        pixels = b"".join(rows)
    else:
        pixels = bytearray()
        for row in rows:
            for i in range(0, row_bytes, 3):
                rgb = row[i : i + 3]
                alpha = 0 if transparent_key == tuple(rgb) else 255
                pixels.extend(rgb + bytes((alpha,)))
        pixels = bytes(pixels)
    return PngImage(width, height, pixels)


def _validate_crop(crop: Crop, width: int, height: int) -> None:
    if len(crop) != 4 or any(not isinstance(value, int) for value in crop):
        raise ValueError("crop must contain four integers")
    x, y, crop_width, crop_height = crop
    if any(value < 0 for value in crop):
        raise ValueError("crop values must be >= 0")
    if x + crop_width > width or y + crop_height > height:
        raise ValueError("crop region must be within image bounds")


def _crop_image(image: PngImage, crop: Crop) -> PngImage:
    x, y, crop_width, crop_height = crop
    row_size = image.width * 4
    cropped_row_size = crop_width * 4
    pixels = b"".join(
        image.pixels[(y + row) * row_size + x * 4 : (y + row) * row_size + (x * 4) + cropped_row_size]
        for row in range(crop_height)
    )
    return PngImage(crop_width, crop_height, pixels)


def compare_pngs(
    reference: str | Path, actual: str | Path, threshold: int = 0, crop: Crop | None = None
) -> Comparison:
    """Compare two PNGs; threshold is the permitted per-pixel max channel error."""
    if threshold < 0 or threshold > 255:
        raise ValueError("threshold must be between 0 and 255")
    expected = read_png(reference)
    observed = read_png(actual)
    if (expected.width, expected.height) != (observed.width, observed.height):
        return Comparison(expected.width, expected.height, 0, 1.0, -1, False)
    if crop is not None:
        _validate_crop(crop, expected.width, expected.height)
        expected = _crop_image(expected, crop)
        observed = _crop_image(observed, crop)
    differing = 0
    maximum = 0
    for left, right in zip(expected.pixels, observed.pixels):
        maximum = max(maximum, abs(left - right))
    for i in range(0, len(expected.pixels), 4):
        if max(abs(expected.pixels[i + j] - observed.pixels[i + j]) for j in range(4)) > threshold:
            differing += 1
    total = expected.width * expected.height
    return Comparison(expected.width, expected.height, differing, differing / total if total else 0.0, maximum)


def write_diff_png(
    reference: str | Path, actual: str | Path, output: str | Path, crop: Crop | None = None
) -> None:
    expected, observed = read_png(reference), read_png(actual)
    if (expected.width, expected.height) != (observed.width, observed.height):
        raise ValueError("cannot write diff PNG for images with different dimensions")
    if crop is not None:
        _validate_crop(crop, expected.width, expected.height)
        expected = _crop_image(expected, crop)
        observed = _crop_image(observed, crop)
    pixels = bytearray()
    for i in range(0, len(expected.pixels), 4):
        error = max(abs(expected.pixels[i + j] - observed.pixels[i + j]) for j in range(4))
        pixels.extend((error, 0, 0, 255 if error else 0))
    raw = b"".join(b"\x00" + bytes(pixels[y * expected.width * 4 : (y + 1) * expected.width * 4]) for y in range(expected.height))
    def chunk(kind: bytes, payload: bytes) -> bytes:
        return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", binascii.crc32(kind + payload) & 0xFFFFFFFF)
    ihdr = struct.pack(">IIBBBBB", expected.width, expected.height, 8, 6, 0, 0, 0)
    Path(output).write_bytes(PNG_SIGNATURE + chunk(b"IHDR", ihdr) + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b""))
