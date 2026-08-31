import binascii
import struct
import tempfile
import unittest
import zlib
import subprocess
import sys
import json
from pathlib import Path

from png_compare import compare_pngs, read_png, write_diff_png


def write_test_png(path, width, height, pixels, color_type=6, trns=None, filters=None):
    channels = 4 if color_type == 6 else 3
    rows = [pixels[y * width * channels : (y + 1) * width * channels] for y in range(height)]
    encoded_rows = []
    previous = bytes(width * channels)
    for y, row in enumerate(rows):
        filter_type = 0 if filters is None else filters[y]
        encoded = bytearray()
        for i, value in enumerate(row):
            left = row[i - channels] if i >= channels else 0
            up = previous[i]
            upper_left = previous[i - channels] if i >= channels else 0
            if filter_type == 0:
                filtered = value
            elif filter_type == 1:
                filtered = (value - left) & 255
            elif filter_type == 2:
                filtered = (value - up) & 255
            elif filter_type == 3:
                filtered = (value - ((left + up) // 2)) & 255
            elif filter_type == 4:
                estimate = left + up - upper_left
                pa, pb, pc = abs(estimate - left), abs(estimate - up), abs(estimate - upper_left)
                predictor = left if pa <= pb and pa <= pc else (up if pb <= pc else upper_left)
                filtered = (value - predictor) & 255
            else:
                raise ValueError("invalid test filter")
            encoded.append(filtered)
        encoded_rows.append(bytes((filter_type,)) + bytes(encoded))
        previous = row
    raw = b"".join(encoded_rows)

    def chunk(kind, payload):
        return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", binascii.crc32(kind + payload) & 0xFFFFFFFF)

    header = struct.pack(">IIBBBBB", width, height, 8, color_type, 0, 0, 0)
    trns_chunk = b"" if trns is None else chunk(b"tRNS", struct.pack(">HHH", *trns))
    path.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + trns_chunk + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b""))


class PngCompareTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)

    def tearDown(self):
        self.temp.cleanup()

    def test_identical_rgb_and_rgba_are_equal(self):
        rgb = self.root / "rgb.png"
        rgba = self.root / "rgba.png"
        write_test_png(rgb, 1, 1, bytes((10, 20, 30)), color_type=2)
        write_test_png(rgba, 1, 1, bytes((10, 20, 30, 255)))
        result = compare_pngs(rgb, rgba)
        self.assertEqual((result.differing_pixels, result.max_error), (0, 0))

    def test_rgb_trns_color_key_decodes_to_rgba(self):
        image = self.root / "transparent.png"
        write_test_png(image, 2, 1, bytes((10, 20, 30, 40, 50, 60)), color_type=2, trns=(10, 20, 30))
        self.assertEqual(read_png(image).pixels, bytes((10, 20, 30, 0, 40, 50, 60, 255)))

    def test_all_png_filters_decode(self):
        image = self.root / "filtered.png"
        source = bytes((10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120))
        write_test_png(image, 2, 2, source, color_type=2, filters=[0, 1])
        self.assertEqual(read_png(image).pixels, b"".join(bytes(source[i : i + 3]) + b"\xff" for i in range(0, len(source), 3)))

        image = self.root / "filtered_all.png"
        source = bytes(range(30))
        write_test_png(image, 2, 5, source, color_type=2, filters=[0, 1, 2, 3, 4])
        self.assertEqual(read_png(image).pixels, b"".join(bytes(source[i : i + 3]) + b"\xff" for i in range(0, len(source), 3)))

    def test_size_mismatch_is_reported(self):
        first, second = self.root / "first.png", self.root / "second.png"
        write_test_png(first, 1, 1, bytes((0, 0, 0, 255)))
        write_test_png(second, 2, 1, bytes((0, 0, 0, 255, 0, 0, 0, 255)))
        result = compare_pngs(first, second)
        self.assertEqual((result.width, result.height, result.differing_pixels, result.same_size), (1, 1, 0, False))

    def test_threshold_counts_pixels_over_error(self):
        reference, actual = self.root / "reference.png", self.root / "actual.png"
        write_test_png(reference, 2, 1, bytes((10, 10, 10, 255, 10, 10, 10, 255)))
        write_test_png(actual, 2, 1, bytes((12, 10, 10, 255, 20, 10, 10, 255)))
        result = compare_pngs(reference, actual, threshold=2)
        self.assertEqual(result.differing_pixels, 1)
        self.assertEqual(result.max_error, 10)
        self.assertAlmostEqual(result.differing_ratio, 0.5)

    def test_crop_compares_only_selected_region(self):
        reference, actual = self.root / "reference.png", self.root / "actual.png"
        write_test_png(reference, 3, 2, bytes((0, 0, 0, 255) * 6))
        pixels = bytearray((0, 0, 0, 255) * 6)
        pixels[4 * 4] = 100
        write_test_png(actual, 3, 2, bytes(pixels))
        result = compare_pngs(reference, actual, crop=(1, 1, 1, 1))
        self.assertEqual((result.width, result.height, result.differing_pixels), (1, 1, 1))
        self.assertEqual(compare_pngs(reference, actual, crop=(0, 0, 1, 1)).differing_pixels, 0)

    def test_crop_must_be_inside_image(self):
        image = self.root / "image.png"
        write_test_png(image, 2, 2, bytes((0, 0, 0, 255) * 4))
        with self.assertRaisesRegex(ValueError, "within image bounds"):
            compare_pngs(image, image, crop=(1, 1, 2, 1))

    def test_cli_success_failure_and_invalid_limits(self):
        reference = self.root / "reference.png"
        actual = self.root / "actual.png"
        write_test_png(reference, 1, 1, bytes((0, 0, 0, 255)))
        write_test_png(actual, 1, 1, bytes((10, 0, 0, 255)))
        script = Path(__file__).parents[1] / "compare_png.py"
        command = [sys.executable, str(script), str(reference), str(actual)]
        self.assertEqual(subprocess.run(command + ["--max-diff-pixels", "1", "--max-diff-ratio", "1"], capture_output=True).returncode, 0)
        self.assertEqual(subprocess.run(command, capture_output=True).returncode, 1)
        for option, value in (("--max-diff-pixels", "-1"), ("--max-diff-ratio", "1.1"), ("--max-diff-ratio", "nan")):
            completed = subprocess.run(command + [option, value], capture_output=True, text=True)
            self.assertEqual(completed.returncode, 2)
            self.assertIn("usage:", completed.stderr)

    def test_cli_crop_json_and_diff_use_cropped_dimensions(self):
        reference, actual, diff = self.root / "reference.png", self.root / "actual.png", self.root / "diff.png"
        write_test_png(reference, 2, 2, bytes((0, 0, 0, 255) * 4))
        pixels = bytearray((0, 0, 0, 255) * 4)
        pixels[3 * 4] = 100
        write_test_png(actual, 2, 2, bytes(pixels))
        script = Path(__file__).parents[1] / "compare_png.py"
        completed = subprocess.run(
            [sys.executable, str(script), str(reference), str(actual), "--crop", "1,1,1,1", "--diff", str(diff)],
            capture_output=True, text=True,
        )
        self.assertEqual(completed.returncode, 1)
        self.assertEqual(json.loads(completed.stdout)["crop"], [1, 1, 1, 1])
        diff_image = read_png(diff)
        self.assertEqual((diff_image.width, diff_image.height), (1, 1))
        self.assertEqual(diff_image.pixels, bytes((100, 0, 0, 255)))
        completed = subprocess.run(
            [sys.executable, str(script), str(reference), str(actual), "--crop", "2,1,1,1"],
            capture_output=True, text=True,
        )
        self.assertEqual(completed.returncode, 2)
        self.assertIn("within image bounds", completed.stderr)

    def test_cli_rejects_invalid_crop(self):
        image = self.root / "image.png"
        write_test_png(image, 1, 1, bytes((0, 0, 0, 255)))
        script = Path(__file__).parents[1] / "compare_png.py"
        completed = subprocess.run(
            [sys.executable, str(script), str(image), str(image), "--crop", "0,0,-1,1"],
            capture_output=True, text=True,
        )
        self.assertEqual(completed.returncode, 2)
        self.assertIn("usage:", completed.stderr)

    def test_diff_output_is_a_readable_red_heatmap(self):
        reference, actual, diff = self.root / "reference.png", self.root / "actual.png", self.root / "diff.png"
        write_test_png(reference, 2, 1, bytes((0, 0, 0, 255, 0, 0, 0, 255)))
        write_test_png(actual, 2, 1, bytes((0, 0, 0, 255, 100, 20, 0, 255)))
        write_diff_png(reference, actual, diff)
        image = read_png(diff)
        self.assertEqual(image.pixels, bytes((0, 0, 0, 0, 100, 0, 0, 255)))


if __name__ == "__main__":
    unittest.main()
