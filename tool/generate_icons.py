"""Generate launcher icons from source PNG."""
from pathlib import Path

from PIL import Image

SOURCE = Path(r"C:\Users\wital\Downloads\Working Time\Bilder\ChatGPT Image 21. Juli 2026, 11_45_02.png")
RES = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "res"

SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

FOREGROUND_SIZE = 432


def resize_square(img: Image.Image, size: int) -> Image.Image:
    return img.resize((size, size), Image.Resampling.LANCZOS)


def main() -> None:
    img = Image.open(SOURCE).convert("RGBA")

    drawable_dir = RES / "drawable-nodpi"
    drawable_dir.mkdir(parents=True, exist_ok=True)
    foreground = resize_square(img, FOREGROUND_SIZE)
    foreground.save(drawable_dir / "ic_launcher_foreground.png")

    for folder, size in SIZES.items():
        out_dir = RES / folder
        out_dir.mkdir(parents=True, exist_ok=True)
        icon = resize_square(img, size)
        icon.save(out_dir / "ic_launcher.webp", format="WEBP", quality=95)
        icon.save(out_dir / "ic_launcher_round.webp", format="WEBP", quality=95)

    playstore = RES.parent / "ic_launcher-playstore.png"
    resize_square(img, 512).save(playstore, format="PNG")

    print("Icons generated.")


if __name__ == "__main__":
    main()