import unittest

from probe_parse import last_probe_from_text, parse_probe_line


class ProbeParseTest(unittest.TestCase):
    def test_parse_line(self):
        p = parse_probe_line('SPIREUI_PROBE {"schemaVersion":1,"modId":"spireui"}')
        self.assertEqual(p["schemaVersion"], 1)
        self.assertEqual(p["modId"], "spireui")

    def test_last_from_log(self):
        text = """
info foo
SPIREUI_PROBE {"schemaVersion":1,"n":1}
noise
SPIREUI_PROBE {"schemaVersion":1,"n":2}
"""
        p = last_probe_from_text(text)
        self.assertEqual(p["n"], 2)


if __name__ == "__main__":
    unittest.main()
