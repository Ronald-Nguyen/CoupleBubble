"""Builds app/src/main/assets/cities1000.tsv.gz, the offline city list for the reunion distance.

Source: GeoNames cities1000 (every place with at least 1,000 inhabitants), https://download.geonames.org/export/dump/cities1000.zip
License: Creative Commons Attribution 4.0 (https://creativecommons.org/licenses/by/4.0/), credited in the app and the README.

Usage: python tools/build-city-index.py path/to/cities1000.txt

Output columns (tab separated, one city per line, sorted by population, largest first):
name, asciiname, country code, latitude, longitude, population, latin alternate names (comma separated)
Only Latin-script alternate names are kept, which keeps the file small and covers German spellings like "Köln".
"""

import gzip
import pathlib
import re
import sys

OUTPUT = pathlib.Path(__file__).resolve().parent.parent / "app/src/main/assets/cities1000.tsv.gz"

# Basic Latin, Latin-1, Latin Extended-A/B, Latin Extended Additional, common punctuation
LATIN_ONLY = re.compile(r"^[\u0000-ɏḀ-ỿ‐-‧·]+$")
CLEAN = re.compile(r"[\t\r\n]+")


def clean(value: str) -> str:
    return CLEAN.sub(" ", value).strip()


def main() -> None:
    if len(sys.argv) != 2:
        sys.exit(__doc__)

    rows = []
    with open(sys.argv[1], encoding="utf-8") as source:
        for line in source:
            columns = line.rstrip("\n").split("\t")
            if len(columns) < 15 or not columns[1]:
                continue
            name, ascii_name, country, lat, lng, population = (
                clean(columns[1]), clean(columns[2]), columns[8], columns[4], columns[5], columns[14]
            )
            alternates = [clean(a) for a in columns[3].split(",") if a and LATIN_ONLY.match(clean(a))]
            rows.append((int(population or 0), name, ascii_name, country, lat, lng, ",".join(alternates)))

    rows.sort(key=lambda row: -row[0])
    lines = ["\t".join([name, ascii_name, country, lat, lng, str(pop), alt])
             for pop, name, ascii_name, country, lat, lng, alt in rows]
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with gzip.open(OUTPUT, "wt", encoding="utf-8", compresslevel=9) as target:
        target.write("\n".join(lines) + "\n")
    print(f"{len(lines)} cities -> {OUTPUT} ({OUTPUT.stat().st_size / 1e6:.1f} MB)")


if __name__ == "__main__":
    main()
