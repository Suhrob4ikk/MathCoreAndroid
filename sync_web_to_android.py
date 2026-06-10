#!/usr/bin/env python3
"""Sync web JS questions to Android JSON files.
Old format: question:/options:/correct:
New format: q:/o:/a: (linalg, probability)
"""
import re, json, os, sys

WEB = "C:/Users/Suhrob/Documents/site/Calculus/Calculus/js/"
AND = "C:/Users/Suhrob/Documents/MathCoreAndroid/app/src/main/assets/questions/"

# ─── normalization ────────────────────────────────────────────────────────────

def norm(s):
    """Normalize question text for duplicate detection."""
    s = re.sub(r'\$', '', s)
    s = re.sub(r'\\\(', '', s); s = re.sub(r'\\\)', '', s)
    s = re.sub(r'функции?|функцию', '', s, flags=re.I)
    s = re.sub(r'\s+', ' ', s).strip().lower()
    return s

def strip_dollar(s):
    s = s.strip()
    if s.startswith('$') and s.endswith('$'):
        return s[1:-1].strip()
    return s

def strip_paren(s):
    s = re.sub(r'\\\(', '', s)
    s = re.sub(r'\\\)', '', s)
    return s.strip()

# ─── parsers ──────────────────────────────────────────────────────────────────

def parse_old_format(filepath):
    """question:/options:/correct: (derivatives, integrals, limits, ode, series)."""
    content = open(filepath, encoding='utf-8').read()
    sections = {}

    for em in re.finditer(r'export\s+const\s+(\w+)\s*=\s*\[', content):
        name = em.group(1)
        start = content.index('[', em.start())
        depth = 0
        pos = start
        while pos < len(content):
            if content[pos] == '[': depth += 1
            elif content[pos] == ']':
                depth -= 1
                if depth == 0: break
            pos += 1
        block = content[start:pos + 1]

        qs = []
        for m in re.finditer(
            r'question:\s*"((?:[^"\\]|\\.)*)"'
            r'\s*,\s*options:\s*\[(.*?)\]'
            r'\s*,\s*correct:\s*(\d+)',
            block, re.DOTALL
        ):
            q_text = m.group(1).replace('\\"', '"').replace('\\n', ' ')
            opts_raw = m.group(2)
            correct  = int(m.group(3))
            opts = re.findall(r'"((?:[^"\\]|\\.)*)"', opts_raw)
            opts = [strip_dollar(o.replace('\\"', '"')) for o in opts]
            if len(opts) == 4:
                qs.append({'question': q_text, 'options': opts, 'correct': correct})
        sections[name] = qs
    return sections


def parse_new_format(filepath):
    """q:/o:/a: format (linalg, probability)."""
    content = open(filepath, encoding='utf-8').read()
    sections = {}

    for level in ['easy', 'medium', 'hard']:
        m = re.search(rf'\b{level}\s*:\s*\[', content)
        if not m:
            sections[level] = []; continue

        start = content.rindex('[', m.start(), m.end() + 1)
        depth = 0; pos = start
        while pos < len(content):
            if content[pos] == '[': depth += 1
            elif content[pos] == ']':
                depth -= 1
                if depth == 0: break
            pos += 1
        block = content[start:pos + 1]

        qs = []
        # pattern handles multiline q value
        for qm in re.finditer(
            r"\{q:'((?:[^'\\]|\\.)*)'\s*[,\n]\s*o:\[(.*?)\]\s*,a:(\d+)",
            block, re.DOTALL
        ):
            raw_q = qm.group(1).replace("\\'", "'")
            q_text = re.sub(r'\\\(', '$', raw_q)
            q_text = re.sub(r'\\\)', '$', q_text)

            opts_raw = qm.group(2)
            correct  = int(qm.group(3))
            opts = re.findall(r"'((?:[^'\\]|\\.)*)'", opts_raw)
            opts = [strip_paren(o.replace("\\'", "'")) for o in opts]
            if len(opts) == 4:
                qs.append({'question': q_text, 'options': opts, 'correct': correct})
        sections[level] = qs
    return sections

# ─── I/O ─────────────────────────────────────────────────────────────────────

def load_android(path):
    if not os.path.exists(path): return []
    return json.load(open(path, encoding='utf-8'))

def opts_key(q):
    """Options fingerprint: sorted joined options + correct index."""
    return (tuple(sorted(q['options'])), q['correct'])

def find_new(web_qs, android_qs, use_opts=False):
    existing_text = {norm(q['question']) for q in android_qs}
    existing_opts = {opts_key(q) for q in android_qs} if use_opts else set()
    result = []
    for q in web_qs:
        if norm(q['question']) in existing_text:
            continue
        if use_opts and opts_key(q) in existing_opts:
            continue
        result.append(q)
    return result

def write_android(path, existing, new_qs):
    all_qs = existing + new_qs
    lines = ['[']
    for i, q in enumerate(all_qs):
        line = json.dumps(q, ensure_ascii=False)
        if i < len(all_qs) - 1:
            line += ','
        lines.append(line)
    lines.append(']')
    open(path, 'w', encoding='utf-8', newline='\n').write('\n'.join(lines) + '\n')

# ─── topic maps ───────────────────────────────────────────────────────────────

OLD_TOPICS = {
    'derivatives-questions.js': {
        'easyDerivativesQuestions':   'derivatives_easy',
        'mediumDerivativesQuestions': 'derivatives_medium',
        'hardDerivativesQuestions':   'derivatives_hard',
    },
    'integrals-questions.js': {
        'easyIntegralsQuestions':   'integrals_easy',
        'mediumIntegralsQuestions': 'integrals_medium',
        'hardIntegralsQuestions':   'integrals_hard',
    },
    'limits-questions.js': {
        'easyLimitsQuestions':   'limits_easy',
        'mediumLimitsQuestions': 'limits_medium',
        'hardLimitsQuestions':   'limits_hard',
    },
    'ode-questions.js': {
        'easyODEQuestions':   'ode_easy',
        'mediumODEQuestions': 'ode_medium',
        'hardODEQuestions':   'ode_hard',
    },
    'series-questions.js': {
        'easySeriesQuestions':   'series_easy',
        'mediumSeriesQuestions': 'series_medium',
        'hardSeriesQuestions':   'series_hard',
    },
}

NEW_TOPICS = {
    'linalg-questions.js':      'linalg',
    'probability-questions.js': 'probability',
}

# ─── main ────────────────────────────────────────────────────────────────────

total_added = 0
report = []

print("=== SINCHRONIZACIJA WEB -> ANDROID ===\n")

for js_file, mapping in OLD_TOPICS.items():
    js_path = WEB + js_file
    try:
        sections = parse_old_format(js_path)
    except Exception as e:
        print(f"  ERROR parsing {js_file}: {e}"); continue

    for export_name, android_prefix in mapping.items():
        web_qs = sections.get(export_name, [])
        android_path = AND + android_prefix + '.json'
        android_qs   = load_android(android_path)
        new_qs = find_new(web_qs, android_qs)

        if new_qs:
            write_android(android_path, android_qs, new_qs)
            msg = f"  {android_prefix}: +{len(new_qs)} (было {len(android_qs)} → стало {len(android_qs)+len(new_qs)})"
            total_added += len(new_qs)
        else:
            msg = f"  {android_prefix}: без изменений ({len(android_qs)} вопросов)"
        print(msg)
        report.append(msg)

print()

for js_file, topic in NEW_TOPICS.items():
    js_path = WEB + js_file
    try:
        sections = parse_new_format(js_path)
    except Exception as e:
        print(f"  ERROR parsing {js_file}: {e}"); continue

    for level, web_qs in sections.items():
        android_prefix = f"{topic}_{level}"
        android_path   = AND + android_prefix + '.json'
        android_qs     = load_android(android_path)
        new_qs = find_new(web_qs, android_qs, use_opts=True)

        if new_qs:
            write_android(android_path, android_qs, new_qs)
            msg = f"  {android_prefix}: +{len(new_qs)} (было {len(android_qs)} → стало {len(android_qs)+len(new_qs)})"
            total_added += len(new_qs)
        else:
            msg = f"  {android_prefix}: без изменений ({len(android_qs)} вопросов)"
        print(msg)
        report.append(msg)

print(f"\n{'='*50}")
print(f"Итого добавлено: {total_added} вопросов")
