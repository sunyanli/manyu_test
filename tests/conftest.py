"""Ensure the repository root is importable when pytest is run without `-m`."""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
