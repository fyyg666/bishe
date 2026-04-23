import os
import re
import glob

def clean_fixed_comments(file_path):
    """清理FIXED注释，保留有价值的说明性注释"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()

        original = content

        # 匹配模式: // FIXED: XXX 或 // FIXED - XXX 及其后续内容直到行尾
        # 保留行首的注释（不是FIXED标记的）
        pattern = r'//\s*FIXED[:\s-][^\n]*'

        content = re.sub(pattern, '', content)

        # 清理连续的空注释行
        content = re.sub(r'\n\s*\n\s*\n', '\n\n', content)

        if content != original:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            return True
        return False
    except Exception as e:
        print(f"Error processing {file_path}: {e}")
        return False

def main():
    base_path = r"c:/Users/12856/Desktop/论文实现/library-system-v2/backend/src/main/java/com/library/system"

    files_processed = 0
    files_modified = 0

    for java_file in glob.glob(os.path.join(base_path, "**/*.java"), recursive=True):
        files_processed += 1
        if clean_fixed_comments(java_file):
            files_modified += 1
            print(f"Modified: {java_file}")

    print(f"\nSummary:")
    print(f"  Files processed: {files_processed}")
    print(f"  Files modified: {files_modified}")

if __name__ == "__main__":
    main()
