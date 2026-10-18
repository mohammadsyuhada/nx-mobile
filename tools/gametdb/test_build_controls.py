"""Run: python3 -I tools/gametdb/test_build_controls.py (from the repo root)."""
import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import build_controls  # noqa: E402

SAMPLE = """<?xml version="1.0" encoding="UTF-8"?>
<datafile>
  <WiiTDB version="20261013031249" games="4"/>
  <game name="NSMB"><id>SMNE01</id><type></type>
    <input players="4"><control type="wiimote" required="true"/><control type="nunchuk" required="false"/></input></game>
  <game name="MKWii"><id>RMCE01</id><type></type>
    <input players="4"><control type="wiimote" required="true"/><control type="classiccontroller" required="false"/></input></game>
  <game name="Skyward"><id>SOUE01</id><type></type>
    <input players="1"><control type="wiimote" required="true"/><control type="nunchuk" required="true"/><control type="motionplus" required="true"/></input></game>
  <game name="Wii Sports"><id>RSPE01</id><type></type>
    <input players="4"><control type="wiimote" required="true"/></input></game>
  <game name="Homebrew"><id>ABCD12</id><type>CUSTOM</type>
    <input players="1"><control type="wiimote" required="true"/></input></game>
</datafile>
"""


class BuildControlsTest(unittest.TestCase):
    def rows(self, upright=("RSP",)):
        return build_controls.rows(SAMPLE, set(upright))

    def test_flags_per_game(self):
        self.assertEqual(
            [("RMCE01", "C"), ("RSPE01", "U"), ("SMNE01", ""), ("SOUE01", "NM")],
            self.rows(),
        )

    def test_optional_nunchuk_and_optional_motionplus_set_no_flag(self):
        self.assertEqual("", dict(self.rows())["SMNE01"])

    def test_custom_entries_are_left_out(self):
        self.assertNotIn("ABCD12", dict(self.rows()))

    def test_render_has_the_version_header_and_tab_rows(self):
        text = build_controls.render(SAMPLE, {"RSP"})
        lines = text.splitlines()
        self.assertEqual("# GameTDB wiitdb 20261013031249, https://www.gametdb.com", lines[0])
        self.assertIn("RSPE01\tU", lines)
        self.assertTrue(text.endswith("\n"))

    def test_a_file_declaring_entities_is_refused(self):
        evil = '<?xml version="1.0"?><!DOCTYPE d [<!ENTITY a "aaaa">]><datafile><WiiTDB version="1"/></datafile>'
        with self.assertRaises(ValueError):
            build_controls.rows(evil, set())

    def test_upright_file_parsing_skips_comments_and_names(self):
        self.assertEqual({"RSP", "RTZ"}, build_controls.parse_upright("# c\nRSP\tWii Sports\n\nRTZ\tZack & Wiki\n"))


if __name__ == "__main__":
    unittest.main()
