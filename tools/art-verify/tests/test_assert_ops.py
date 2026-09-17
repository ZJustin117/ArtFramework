import unittest

from assert_ops import AssertError, resolve_path, run_assert


class AssertOpsTest(unittest.TestCase):
    def test_resolve_dot_and_index(self):
        data = {"a": {"b": [{"c": 1}, {"c": 2}]}}
        ok, v = resolve_path(data, "a.b[1].c")
        self.assertTrue(ok)
        self.assertEqual(v, 2)

    def test_eq_and_contains(self):
        data = {"schemaVersion": 1, "ids": ["demo", "map"]}
        run_assert(data, {"path": "schemaVersion", "eq": 1})
        run_assert(data, {"path": "ids", "contains": "demo"})
        with self.assertRaises(AssertError):
            run_assert(data, {"path": "schemaVersion", "eq": 2})

    def test_exists(self):
        data = {"x": 1}
        run_assert(data, {"path": "x", "exists": True})
        run_assert(data, {"path": "y", "exists": False})

    def test_gt_var_compares_numeric_values(self):
        run_assert({"count": 2.5}, {"path": "count", "gt_var": "before"}, vars={"before": 2})
        with self.assertRaisesRegex(AssertError, "op=gt_var"):
            run_assert({"count": 2}, {"path": "count", "gt_var": "before"}, vars={"before": 2})

    def test_gt_var_rejects_missing_and_non_numeric_variables(self):
        with self.assertRaisesRegex(AssertError, "missing var"):
            run_assert({"count": 2}, {"path": "count", "gt_var": "before"})
        with self.assertRaisesRegex(AssertError, "requires numeric"):
            run_assert(
                {"count": "3"},
                {"path": "count", "gt_var": "before"},
                vars={"before": 2},
            )
        with self.assertRaisesRegex(AssertError, "requires numeric"):
            run_assert(
                {"count": 3},
                {"path": "count", "gt_var": "before"},
                vars={"before": "2"},
            )


if __name__ == "__main__":
    unittest.main()
