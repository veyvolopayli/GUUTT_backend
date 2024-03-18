import sys
import ddddocr


def solve_captcha(path: str) -> str:
    ocr = ddddocr.DdddOcr()

    with open(path, "rb") as f:
        image = f.read()

    return ocr.classification(image)


if __name__ == '__main__':
    captcha_file_name = "py/" + sys.argv[1]
    # print()
    with open(captcha_file_name.replace(".png", ".txt"), 'w', encoding='utf-8') as file:
        file.write(solve_captcha(captcha_file_name))
