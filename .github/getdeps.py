import subprocess
from pathlib import Path


def get_dll_dependencies(dll_path):
    try:
        result = subprocess.run(['dumpbin', '/dependents', dll_path],
                                capture_output=True, text=True, check=True)
        dependencies = []
        for line in result.stdout.splitlines():
            line = line.strip()
            if line.lower().endswith('.dll'):
                dependencies.append(line)
        return dependencies
    except subprocess.CalledProcessError:
        return []


def scan_current_directory():
    current_dir = Path('.')
    dll_files = [f for f in current_dir.glob('*.dll')]

    dependency_map = {}
    for dll in dll_files:
        deps = get_dll_dependencies(str(dll))
        existing_deps = [d for d in deps if Path(d).exists()]
        if existing_deps:
            dependency_map[dll.name] = existing_deps

    with open('java-gi-meta-v1.txt', 'w') as f:
        for dll, deps in dependency_map.items():
            f.write(f"{dll}: {', '.join(deps)}\n")


if __name__ == '__main__':
    scan_current_directory()
