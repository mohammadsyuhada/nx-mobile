"""Local check of nxtest_targets (no device): python3 -m unittest test_nxtest_targets, from this folder."""

import unittest

from brick import nxtest_targets


class NxtestTargets(unittest.TestCase):
    def test_a_core_folder_named_after_the_game_goes_whole(self):
        files = "/s/Saves/PRBOOM/nxtest-freedoom1/prboom.cfg\n/s/Saves/GB/nxtest-a.srm\n"
        dirs = "/s/Saves/PRBOOM/nxtest-freedoom1\n"
        self.assertEqual(["/s/Saves/PRBOOM/nxtest-freedoom1/", "/s/Saves/GB/nxtest-a.srm"], nxtest_targets(files, dirs))

    def test_only_the_nxtest_prefix_counts(self):
        files = "/s/Saves/GB/real.srm\n/s/Saves/GB/my-nxtest-a.srm\n\n"
        dirs = "/s/Saves/PRBOOM/freedoom1\n/s/Saves/nxtest\n"
        self.assertEqual([], nxtest_targets(files, dirs))

    def test_nested_matches_are_not_listed_twice(self):
        files = "/s/nxtest-a/nxtest-b/nxtest-c.sav\n/s/nxtest-a/x\n"
        dirs = "/s/nxtest-a/nxtest-b\n/s/nxtest-a/\n"
        self.assertEqual(["/s/nxtest-a/"], nxtest_targets(files, dirs))

    def test_a_sibling_with_a_longer_name_is_not_inside(self):
        files = "/s/nxtest-ab.srm\n"
        dirs = "/s/nxtest-a\n"
        self.assertEqual(["/s/nxtest-a/", "/s/nxtest-ab.srm"], nxtest_targets(files, dirs))

    def test_files_alone_keep_their_order(self):
        self.assertEqual(["/s/nxtest-b", "/s/nxtest-a"], nxtest_targets("/s/nxtest-b\n/s/nxtest-a\n/s/nxtest-b\n", ""))


if __name__ == "__main__":
    unittest.main()
