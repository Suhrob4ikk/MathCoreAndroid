package com.mathcore.app.data

data class TheoryStep(
    val title: String,
    val formula: String,
    val note: String,
    val detail: String
)

data class TheorySubject(
    val key: String,
    val title: String,
    val icon: String,
    val accentColorHex: String,
    val steps: List<TheoryStep>
)

fun Subject.theoryKey(): String = when (this) {
    Subject.INTEGRALS   -> "integrals"
    Subject.DERIVATIVES -> "derivatives"
    Subject.LIMITS      -> "limits"
    Subject.SERIES      -> "series"
    Subject.ODE         -> "ode"
    Subject.PROBABILITY -> "probability"
    Subject.LINALG      -> "linalg"
    Subject.MIXED       -> "linalg"  // MIXED is duel-only; no dedicated theory page
}

object TheoryData {

    val subjects: Map<String, TheorySubject> = mapOf(

        // ─────────────────────────── ИНТЕГРАЛЫ ───────────────────────────
        "integrals" to TheorySubject(
            key = "integrals", title = "Интегралы", icon = "∫", accentColorHex = "#3b82f6",
            steps = listOf(
                TheoryStep(
                    title = "Степенная функция",
                    formula = """\[\int x^n\,dx = \frac{x^{n+1}}{n+1} + C, \quad n \neq -1\]""",
                    note = """Увеличиваем показатель на 1 и делим на новый показатель. Пример: \(\int x^3\,dx = \frac{x^4}{4}+C\)""",
                    detail = """<p>Берёшь степень <em>n</em>, прибавляешь к ней 1 — новая степень — и на это же число делишь. Работает для любых степеней: целых, дробных, отрицательных. Исключение: \(n=-1\) (деление на ноль).</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(\int x^4\,dx = \dfrac{x^5}{5}+C\)</p>
<p>2)&nbsp; \(\int x^{-3}\,dx = \dfrac{x^{-2}}{-2}+C = -\dfrac{1}{2x^2}+C\)</p>
<p>3)&nbsp; \(\int\sqrt{x}\,dx = \int x^{1/2}\,dx = \dfrac{x^{3/2}}{3/2}+C = \dfrac{2}{3}x^{3/2}+C\)</p>"""
                ),
                TheoryStep(
                    title = "Логарифмический интеграл",
                    formula = """\[\int \frac{1}{x}\,dx = \ln|x| + C\]""",
                    note = """Частный случай при n = −1. Модуль нужен, т.к. ln определён только для x > 0. Пример: \(\int \frac{3}{x}\,dx = 3\ln|x|+C\)""",
                    detail = """<p>При \(n=-1\) формула степенного интеграла не работает (деление на ноль). Результат — натуральный логарифм. Модуль нужен, потому что \(x\) может быть отрицательным.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(\int\dfrac{1}{x}\,dx = \ln|x|+C\)</p>
<p>2)&nbsp; \(\int\dfrac{5}{x}\,dx = 5\ln|x|+C\)</p>
<p>3)&nbsp; \(\int\dfrac{1}{3x}\,dx = \dfrac{1}{3}\ln|x|+C\)</p>"""
                ),
                TheoryStep(
                    title = "Экспонента",
                    formula = """\[\int e^x\,dx = e^x + C \qquad \int a^x\,dx = \frac{a^x}{\ln a} + C\]""",
                    note = """Экспонента — единственная функция, равная своей производной и интегралу. Пример: \(\int e^{2x}\,dx = \frac{1}{2}e^{2x}+C\)""",
                    detail = """<p>Функция \(e^x\) равна своей производной — и, как следствие, интегралу. Для другого основания \(a\) появляется деление на \(\ln a\): при дифференцировании умножается, при интегрировании — делится.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(\int e^x\,dx = e^x+C\)</p>
<p>2)&nbsp; \(\int 3e^x\,dx = 3e^x+C\)</p>
<p>3)&nbsp; \(\int 2^x\,dx = \dfrac{2^x}{\ln 2}+C\)</p>"""
                ),
                TheoryStep(
                    title = "Тригонометрия",
                    formula = """\[\int \sin x\,dx = -\cos x + C \qquad \int \cos x\,dx = \sin x + C\]""",
                    note = """При интегрировании синуса появляется минус! Пример: \(\int \sin 3x\,dx = -\frac{1}{3}\cos 3x+C\)""",
                    detail = """<p>Главная ловушка — <strong>минус у синуса</strong>. Его очень часто забывают! Запомни: интеграл от косинуса — синус (без минуса), интеграл от синуса — минус косинус.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(\int\cos x\,dx = \sin x+C\) — без минуса.</p>
<p>2)&nbsp; \(\int\sin x\,dx = -\cos x+C\) — обязательно с минусом!</p>
<p>3)&nbsp; \(\int 4\sin x\,dx = -4\cos x+C\)</p>"""
                ),
                TheoryStep(
                    title = "Линейная замена",
                    formula = """\[\int f(ax + b)\,dx = \frac{1}{a}\,F(ax + b) + C\]""",
                    note = """Линейный аргумент (ax + b) → делим результат на a. Пример: \(\int (2x+1)^4\,dx = \frac{(2x+1)^5}{10}+C\)""",
                    detail = """<p>Когда внутри стоит \(ax+b\), интегрируем как обычно, но в конце <strong>делим на коэффициент \(a\)</strong>. Это ускоренный метод подстановки для линейных аргументов.</p>
<p>Смысл: замена \(t=ax+b\), \(dt=a\,dx\), то есть \(dx=\frac{dt}{a}\) — вот откуда делитель.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(\int(2x+1)^4\,dx = \dfrac{(2x+1)^5}{10}+C\) — делим на \(a=2\).</p>
<p>2)&nbsp; \(\int\sin(3x)\,dx = -\dfrac{1}{3}\cos(3x)+C\)</p>
<p>3)&nbsp; \(\int e^{5x-2}\,dx = \dfrac{1}{5}e^{5x-2}+C\)</p>"""
                ),
                TheoryStep(
                    title = "Метод подстановки",
                    formula = """\[\int f(g(x))\cdot g'(x)\,dx = F(g(x)) + C\]""",
                    note = """Замена t = g(x), dt = g′(x)dx. Пример: \(\int 2x\cdot e^{x^2}\,dx = e^{x^2}+C\) (t = x²)""",
                    detail = """<p>Ищешь "внутреннюю" функцию \(g(x)\), производная которой тоже стоит под знаком интеграла. Замена \(t=g(x)\), тогда \(dt=g'(x)\,dx\).</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(\int 2x\cdot e^{x^2}\,dx\): замена \(t=x^2\), \(dt=2x\,dx\) → \(\int e^t\,dt = e^{x^2}+C\)</p>
<p>2)&nbsp; \(\int\dfrac{\cos x}{\sin^2 x}\,dx\): \(t=\sin x\) → \(\int t^{-2}\,dt = -\dfrac{1}{\sin x}+C\)</p>
<p>3)&nbsp; \(\int\dfrac{x}{x^2+1}\,dx\): \(t=x^2+1\) → \(\dfrac{\ln(x^2+1)}{2}+C\)</p>"""
                ),
                TheoryStep(
                    title = "Интегрирование по частям",
                    formula = """\[\int u\,dv = uv - \int v\,du\]""",
                    note = """Правило ЛАТЕ: Логарифм, Алгебра, Тригонометрия, Экспонента — выбираем u. Пример: \(\int x e^x\,dx = xe^x - e^x + C\)""",
                    detail = """<p>Используется для произведений функций разных типов. По правилу <strong>ЛАТЕ</strong> выбираем \(u\): Логарифм → Алгебра → Тригонометрия → Экспонента.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(\int xe^x\,dx\): \(u=x\), \(dv=e^x\,dx\) → \(xe^x-e^x+C\)</p>
<p>2)&nbsp; \(\int x\cos x\,dx\): \(u=x\), \(dv=\cos x\,dx\) → \(x\sin x+\cos x+C\)</p>
<p>3)&nbsp; \(\int\ln x\,dx\): \(u=\ln x\), \(dv=dx\) → \(x\ln x-x+C\)</p>"""
                ),
                TheoryStep(
                    title = "Свойства неопределённого интеграла",
                    formula = """\[\int (\alpha f + \beta g)\,dx = \alpha\int f\,dx + \beta\int g\,dx\]""",
                    note = """Интеграл линеен: константу выносим, интеграл суммы — сумма интегралов. Пример: \(\int (3x^2 + 5)\,dx = x^3 + 5x + C\)""",
                    detail = """<p>Интегрирование — линейная операция: константу можно вынести, интеграл суммы равен сумме интегралов. Полиномы интегрируются «по кускам».</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(\int(3x^2+5)\,dx = x^3+5x+C\)</p>
<p>2)&nbsp; \(\int(2e^x-3\cos x)\,dx = 2e^x-3\sin x+C\)</p>
<p>3)&nbsp; \(\int\!\left(x^3-\dfrac{2}{x}+\sqrt{x}\right)\!dx = \dfrac{x^4}{4}-2\ln|x|+\dfrac{2}{3}x^{3/2}+C\)</p>"""
                ),
                TheoryStep(
                    title = "Определённый интеграл (Ньютон–Лейбниц)",
                    formula = """\[\int_a^b f(x)\,dx = F(b) - F(a)\]""",
                    note = """Формула Ньютона–Лейбница: вычисляем первообразную, подставляем пределы. Пример: \(\int_0^1 x^2\,dx = \frac{1}{3}\)""",
                    detail = """<p>Определённый интеграл — «площадь под кривой» от \(a\) до \(b\) (со знаком). Вычислить: найди первообразную \(F(x)\), вычисли \(F(b)-F(a)\).</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(\int_0^1 x^2\,dx = \dfrac{x^3}{3}\Big|_0^1 = \dfrac{1}{3}\)</p>
<p>2)&nbsp; \(\int_0^{\pi} \sin x\,dx = -\cos x\Big|_0^{\pi} = 2\)</p>
<p>3)&nbsp; \(\int_1^e \dfrac{1}{x}\,dx = \ln x\Big|_1^e = 1\)</p>"""
                ),
                TheoryStep(
                    title = "Несобственные интегралы",
                    formula = """\[\int_a^{+\infty} f(x)\,dx = \lim_{b\to+\infty}\int_a^b f(x)\,dx\]""",
                    note = """Несобственный интеграл сходится, если предел конечен. Пример: \(\int_1^{+\infty}\frac{1}{x^2}\,dx = 1\)""",
                    detail = """<p>Когда один из пределов бесконечен или подынтегральная функция имеет разрыв — используем предельный переход.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(\int_1^{+\infty}\dfrac{dx}{x^2} = \lim_{b\to\infty}\left(-\dfrac{1}{b}+1\right)=1\) — сходится.</p>
<p>2)&nbsp; \(\int_1^{+\infty}\dfrac{dx}{x} = \lim_{b\to\infty}\ln b = +\infty\) — расходится.</p>
<p>3)&nbsp; \(\int_0^{+\infty}e^{-x}\,dx = 1\) — сходится.</p>"""
                )
            )
        ),

        // ─────────────────────────── ПРОИЗВОДНЫЕ ───────────────────────────
        "derivatives" to TheorySubject(
            key = "derivatives", title = "Производные", icon = "f'(x)", accentColorHex = "#eab308",
            steps = listOf(
                TheoryStep(
                    title = "Степенная функция",
                    formula = """\[(x^n)'= nx^{n-1}\]""",
                    note = """Степень выносим вперёд, уменьшаем на 1. Примеры: \((x^5)'=5x^4\), \((\sqrt{x})'=\frac{1}{2\sqrt{x}}\)""",
                    detail = """<p>Берёшь степень, умножаешь на неё, и уменьшаешь степень на 1. Работает для любых степеней — целых, дробных, отрицательных.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \((x^5)' = 5x^4\)</p>
<p>2)&nbsp; \((x^3)' = 3x^2\)</p>
<p>3)&nbsp; \((\sqrt{x})' = (x^{1/2})' = \dfrac{1}{2}x^{-1/2} = \dfrac{1}{2\sqrt{x}}\)</p>"""
                ),
                TheoryStep(
                    title = "Экспонента и логарифм",
                    formula = """\[(e^x)'= e^x \qquad (\ln x)'= \frac{1}{x} \qquad (a^x)'= a^x\ln a\]""",
                    note = """Производная eˣ — сам eˣ. Пример: \((e^{3x})'=3e^{3x}\), \((\ln 5x)'=\frac{1}{x}\)""",
                    detail = """<p>Функция \(e^x\) уникальна: производная равна ей самой. Производная \(\ln x\) — это \(\frac{1}{x}\). Знать наизусть!</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \((e^x)' = e^x\)</p>
<p>2)&nbsp; \((\ln x)' = \dfrac{1}{x}\)</p>
<p>3)&nbsp; \((e^{3x})' = 3e^{3x}\) — цепное правило: умножаем на производную \(3x\).</p>"""
                ),
                TheoryStep(
                    title = "Тригонометрия",
                    formula = """\[(\sin x)'= \cos x \qquad (\cos x)'= -\sin x\]""",
                    note = """Производная синуса — косинус (без минуса). Производная косинуса — минус синус.""",
                    detail = """<p>Запомни: \(\sin \to \cos\) (без минуса), \(\cos \to -\sin\) (минус!). Цикл: \(\sin\to\cos\to{-\sin}\to{-\cos}\to\sin\).</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \((\sin x)' = \cos x\) — без минуса.</p>
<p>2)&nbsp; \((\cos x)' = -\sin x\) — минус обязателен!</p>
<p>3)&nbsp; \((3\sin x)' = 3\cos x\)</p>"""
                ),
                TheoryStep(
                    title = "Тангенс и котангенс",
                    formula = """\[(\tan x)'= \frac{1}{\cos^2 x} \qquad (\cot x)'= -\frac{1}{\sin^2 x}\]""",
                    note = """Пример: \((\tan 2x)'=\frac{2}{\cos^2 2x}\). Не забываем цепное правило!""",
                    detail = """<p>Тангенс это \(\frac{\sin x}{\cos x}\), производная выводится через правило частного. Котангенс — аналогично, со знаком минус.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \((\tan x)' = \dfrac{1}{\cos^2 x}\)</p>
<p>2)&nbsp; \((\tan 2x)' = \dfrac{2}{\cos^2 2x}\)</p>
<p>3)&nbsp; \((\cot x)' = -\dfrac{1}{\sin^2 x}\)</p>"""
                ),
                TheoryStep(
                    title = "Правило произведения",
                    formula = """\[(uv)'= u'v + uv'\]""",
                    note = """Каждый множитель дифференцируется по очереди. Пример: \((x^2 e^x)'=2xe^x+x^2 e^x\)""",
                    detail = """<p>Нельзя перемножать производные! Правило: дифференцируешь первый множитель (второй оставляешь), потом второй (первый оставляешь), складываешь.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \((x^2\cdot e^x)' = 2x\cdot e^x + x^2\cdot e^x = xe^x(2+x)\)</p>
<p>2)&nbsp; \((x\cdot\sin x)' = \sin x + x\cos x\)</p>
<p>3)&nbsp; \((x^3\cdot\ln x)' = 3x^2\ln x + x^2\)</p>"""
                ),
                TheoryStep(
                    title = "Правило частного",
                    formula = """\[\left(\frac{u}{v}\right)'= \frac{u'v - uv'}{v^2}\]""",
                    note = """"Числитель минус числитель, делённые на знаменатель²". Пример: \(\left(\frac{x}{e^x}\right)'=\frac{e^x-xe^x}{e^{2x}}\)""",
                    detail = """<p>Производная числителя × знаменатель, <strong>минус</strong> числитель × производная знаменателя, всё делится на знаменатель². Знак минус важен!</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(\left(\dfrac{x}{e^x}\right)' = \dfrac{1\cdot e^x - x\cdot e^x}{e^{2x}} = \dfrac{1-x}{e^x}\)</p>
<p>2)&nbsp; \(\left(\dfrac{\sin x}{x}\right)' = \dfrac{x\cos x - \sin x}{x^2}\)</p>
<p>3)&nbsp; \(\left(\dfrac{x^2}{x+1}\right)' = \dfrac{x^2+2x}{(x+1)^2}\)</p>"""
                ),
                TheoryStep(
                    title = "Цепное правило",
                    formula = """\[(f(g(x)))'= f'(g(x))\cdot g'(x)\]""",
                    note = """Внешняя производная умножается на внутреннюю. Пример: \((\sin x^2)'=\cos(x^2)\cdot 2x\)""",
                    detail = """<p>Берёшь производную <strong>внешней</strong> функции (внутренняя пока не трогаешь), умножаешь на производную <strong>внутренней</strong>.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \((\sin(x^2))' = \cos(x^2)\cdot 2x\)</p>
<p>2)&nbsp; \((e^{3x^2})' = e^{3x^2}\cdot 6x\)</p>
<p>3)&nbsp; \(((x^2+1)^5)' = 5(x^2+1)^4\cdot 2x = 10x(x^2+1)^4\)</p>"""
                ),
                TheoryStep(
                    title = "Обратные тригонометрические",
                    formula = """\[(\arcsin x)'= \frac{1}{\sqrt{1-x^2}} \qquad (\arctan x)'= \frac{1}{1+x^2}\]""",
                    note = """Используются при интегрировании выражений с \(\sqrt{1-x^2}\) и \(1+x^2\).""",
                    detail = """<p>Если видишь в знаменателе \(\sqrt{1-x^2}\) или \(1+x^2\) — ответ скорее всего через арксинус или арктангенс.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \((\arcsin x)' = \dfrac{1}{\sqrt{1-x^2}}\)</p>
<p>2)&nbsp; \((\arctan x)' = \dfrac{1}{1+x^2}\)</p>
<p>3)&nbsp; \((\arctan(2x))' = \dfrac{2}{1+4x^2}\) — цепное правило.</p>"""
                ),
                TheoryStep(
                    title = "Уравнение касательной",
                    formula = """\[y - f(x_0) = f'(x_0)(x - x_0)\]""",
                    note = """Касательная в точке \((x_0, f(x_0))\) имеет наклон \(f'(x_0)\). Нормаль: наклон \(-1/f'(x_0)\).""",
                    detail = """<p>Производная — угловой коэффициент касательной. Уравнение касательной: подставляем точку и наклон.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(f(x)=x^2\) в точке \(x_0=1\): \(f'(1)=2\), касательная: \(y-1=2(x-1)\Rightarrow y=2x-1\)</p>
<p>2)&nbsp; \(f(x)=\sin x\) в точке \(x_0=0\): \(f'(0)=1\), касательная: \(y=x\)</p>
<p>3)&nbsp; Нормаль к \(y=x^2\) при \(x_0=2\): \(f'(2)=4\), нормаль: \(y-4=-\tfrac{1}{4}(x-2)\)</p>"""
                ),
                TheoryStep(
                    title = "Производные высших порядков",
                    formula = """\[f''(x) = (f'(x))' \qquad f^{(n)}(x) = \frac{d^n f}{dx^n}\]""",
                    note = """Вторая производная — ускорение (кривизна). \(f''(x)>0\) — выпуклость, \(f''(x)<0\) — вогнутость.""",
                    detail = """<p>Дифференцируем снова и снова. Вторая производная показывает выпуклость/вогнутость.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(f(x)=x^4\): \(f'=4x^3\), \(f''=12x^2\), \(f'''=24x\), \(f^{(4)}=24\)</p>
<p>2)&nbsp; \((\sin x)^{(n)} = \sin(x+n\pi/2)\) — цикл длины 4.</p>
<p>3)&nbsp; \((e^x)^{(n)} = e^x\) — производные всех порядков.</p>"""
                )
            )
        ),

        // ─────────────────────────── РЯДЫ ───────────────────────────
        "series" to TheorySubject(
            key = "series", title = "Ряды и последовательности", icon = "∑", accentColorHex = "#ef4444",
            steps = listOf(
                TheoryStep(
                    title = "Сходимость ряда",
                    formula = """\[S = \sum_{n=1}^{\infty} a_n = \lim_{N\to\infty} S_N, \quad S_N = a_1 + a_2 + \cdots + a_N\]""",
                    note = """Ряд сходится, если последовательность частичных сумм имеет конечный предел.""",
                    detail = """<p>Ряд — бесконечная сумма. Вопрос: может ли она иметь конечный результат? Да — если слагаемые убывают достаточно быстро.</p>
<p>Составляем частичные суммы \(S_1, S_2, S_3,\ldots\) и смотрим, к чему они стремятся. Конечный предел — ряд <strong>сходится</strong>. Уходит в бесконечность — <strong>расходится</strong>.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(\sum_{n=1}^{\infty}\dfrac{1}{2^n} = 1\) — геометрический ряд с \(q=\tfrac{1}{2}\), сходится.</p>
<p>2)&nbsp; \(\sum_{n=1}^{\infty}\dfrac{1}{n^2} = \dfrac{\pi^2}{6}\) — сходится.</p>
<p>3)&nbsp; \(\sum_{n=1}^{\infty} 1\) — расходится.</p>"""
                ),
                TheoryStep(
                    title = "Необходимый признак расходимости",
                    formula = """\[\lim_{n\to\infty} a_n \neq 0 \implies \sum a_n \text{ расходится}\]""",
                    note = """Если общий член не стремится к нулю — ряд точно расходится. Обратное неверно! Гармонический ряд \(\sum \frac{1}{n}\) расходится, хотя \(\frac{1}{n}\to 0\).""",
                    detail = """<p>Первая проверка: стремится ли \(a_n\) к нулю? Нет — ряд расходится. Осторожно: если \(a_n\to 0\) — это ещё <strong>не значит</strong> сходимости!</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(\sum\dfrac{n}{n+1}\): \(\lim\dfrac{n}{n+1}=1\neq 0\) → расходится.</p>
<p>2)&nbsp; \(\sum(-1)^n\): предел не существует → расходится.</p>
<p>3)&nbsp; \(\sum\dfrac{1}{n}\): \(\lim\dfrac{1}{n}=0\), но ряд расходится!</p>"""
                ),
                TheoryStep(
                    title = "Признак Даламбера",
                    formula = """\[L = \lim_{n\to\infty}\left|\frac{a_{n+1}}{a_n}\right|: \quad L < 1 \Rightarrow \text{сх.}, \quad L > 1 \Rightarrow \text{расх.}, \quad L=1 \Rightarrow \text{?}\]""",
                    note = """Удобен при наличии факториалов и показательных функций. Пример: \(\sum\frac{n!}{n^n}\) — Даламбер даёт L < 1, сходится.""",
                    detail = """<p>Берёшь отношение соседних членов и находишь предел. Хорошо работает с факториалами \(n!\) и степенями \(a^n\) — красиво сокращаются.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(\sum\dfrac{2^n}{n!}\): \(\dfrac{a_{n+1}}{a_n}=\dfrac{2}{n+1}\to 0 < 1\) → сходится.</p>
<p>2)&nbsp; \(\sum\dfrac{n!}{2^n}\): \(\dfrac{a_{n+1}}{a_n}=\dfrac{n+1}{2}\to\infty\) → расходится.</p>
<p>3)&nbsp; \(\sum\dfrac{n^n}{n!}\): \(\to e > 1\) → расходится.</p>"""
                ),
                TheoryStep(
                    title = "Признак Коши (радикальный)",
                    formula = """\[L = \lim_{n\to\infty}\sqrt[n]{|a_n|}: \quad L < 1 \Rightarrow \text{сходится}, \quad L > 1 \Rightarrow \text{расходится}\]""",
                    note = """Удобен, когда aₙ имеет вид (f(n))ⁿ. Пример: \(\sum\left(\frac{2n}{n+1}\right)^n\) — Коши даёт L = 2 > 1, расходится.""",
                    detail = """<p>Берём корень степени \(n\) из \(|a_n|\). Особенно удобен, когда общий член — что-то в степени \(n\). Правило то же, что у Даламбера.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(\sum\left(\dfrac{2n}{n+1}\right)^n\): \(\sqrt[n]{a_n}=\dfrac{2n}{n+1}\to 2 > 1\) → расходится.</p>
<p>2)&nbsp; \(\sum\left(\dfrac{1}{3}\right)^n\): \(\sqrt[n]{a_n}=\dfrac{1}{3} < 1\) → сходится.</p>
<p>3)&nbsp; \(\sum\dfrac{1}{n^n}\): \(\sqrt[n]{a_n}=\dfrac{1}{n}\to 0\) → сходится.</p>"""
                ),
                TheoryStep(
                    title = "Признак Лейбница",
                    formula = """\[\text{Если } |a_n| \searrow 0, \text{ то } \sum_{n=1}^{\infty}(-1)^{n-1}a_n \text{ сходится}\]""",
                    note = """Для знакочередующихся рядов. Пример: \(\sum\frac{(-1)^{n-1}}{n} = \ln 2\)""",
                    detail = """<p>Только для <strong>знакочередующихся</strong> рядов. Условие: модули монотонно убывают к нулю. Интуиция: каждое следующее слагаемое частично "компенсирует" предыдущее.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(\sum\dfrac{(-1)^{n-1}}{n}\): \(a_n=\dfrac{1}{n}\searrow 0\) → сходится, сумма \(= \ln 2\).</p>
<p>2)&nbsp; \(\sum\dfrac{(-1)^n}{n^2}\): \(a_n\searrow 0\) → сходится.</p>
<p>3)&nbsp; \(\sum(-1)^n\): \(a_n=1\) не стремится к нулю → расходится.</p>"""
                ),
                TheoryStep(
                    title = "Признак сравнения",
                    formula = """\[0 \le a_n \le b_n:\quad \sum b_n \text{ сх.} \Rightarrow \sum a_n \text{ сх.}; \quad \sum a_n \text{ расх.} \Rightarrow \sum b_n \text{ расх.}\]""",
                    note = """Сравниваем с эталонным рядом (геометрическим или \(\sum\frac{1}{n^p}\)).""",
                    detail = """<p>Если ряд "меньше" сходящегося — он тоже сходится. Если "больше" расходящегося — расходится. Для сравнения берут эталонные ряды.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(\sum\dfrac{1}{n^2+1}\le\dfrac{1}{n^2}\) и \(\sum\dfrac{1}{n^2}\) сходится → наш тоже.</p>
<p>2)&nbsp; \(\sum\dfrac{1}{\sqrt{n}}\ge\dfrac{1}{n}\) и \(\sum\dfrac{1}{n}\) расходится → наш тоже.</p>
<p>3)&nbsp; \(\sum\dfrac{\sin^2 n}{n^2}\le\dfrac{1}{n^2}\) → сходится.</p>"""
                ),
                TheoryStep(
                    title = "Эталонные ряды",
                    formula = """\[\sum_{n=1}^{\infty}\frac{1}{n^p}: \quad p > 1 \Rightarrow \text{сходится}, \quad p \le 1 \Rightarrow \text{расходится}\]""",
                    note = """Обобщённый гармонический ряд. Пример: \(\sum\frac{1}{n^2}=\frac{\pi^2}{6}\) — сходится; \(\sum\frac{1}{n}\) — расходится.""",
                    detail = """<p>Главный "эталон" для сравнения. Запомни: \(p>1\) — сходится, \(p\le 1\) — расходится. При \(p=2\) сумма равна \(\frac{\pi^2}{6}\) (результат Эйлера).</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(\sum\dfrac{1}{n^2}\) (\(p=2>1\)): сходится, \(=\dfrac{\pi^2}{6}\).</p>
<p>2)&nbsp; \(\sum\dfrac{1}{n}\) (\(p=1\)): расходится — классика!</p>
<p>3)&nbsp; \(\sum\dfrac{1}{\sqrt{n}}=\sum\dfrac{1}{n^{1/2}}\) (\(p=\tfrac{1}{2}\)): расходится.</p>"""
                ),
                TheoryStep(
                    title = "Степенные ряды",
                    formula = """\[\sum_{n=0}^{\infty} c_n(x-a)^n, \quad R = \frac{1}{\limsup|c_n|^{1/n}}\]""",
                    note = """Радиус сходимости R определяет интервал (a−R, a+R), внутри которого ряд сходится абсолютно.""",
                    detail = """<p>Степенной ряд — "многочлен бесконечной степени". Сходится только на интервале вокруг точки \(a\), ширина которого — радиус сходимости \(R\).</p>
<p>Радиус удобно находить признаком Даламбера: \(R=\lim\left|\dfrac{c_n}{c_{n+1}}\right|\).</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(\sum\dfrac{x^n}{n}\): Даламбер даёт \(|x|\to|x|\). Сходится при \(|x|<1\), \(R=1\).</p>
<p>2)&nbsp; \(e^x=\sum\dfrac{x^n}{n!}\): \(R=\infty\), сходится для всех \(x\).</p>
<p>3)&nbsp; \(\sum n!\, x^n\): \(R=0\), сходится только при \(x=0\).</p>"""
                )
            )
        ),

        // ─────────────────────────── ПРЕДЕЛЫ ───────────────────────────
        "limits" to TheorySubject(
            key = "limits", title = "Пределы и непрерывность", icon = "lim", accentColorHex = "#a855f7",
            steps = listOf(
                TheoryStep(
                    title = "Определение предела",
                    formula = """\[\lim_{x \to a} f(x) = L\]""",
                    note = """f(x) приближается к L при x → a. Само значение f(a) может не существовать.""",
                    detail = """<p>Предел описывает, <strong>к чему стремится</strong> функция при приближении \(x\) к точке. Само значение в точке не важно — функция может там не быть определена.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(\lim_{x\to 2}x^2 = 4\) — просто подставляем.</p>
<p>2)&nbsp; \(\lim_{x\to 1}\dfrac{x^2-1}{x-1} = \lim_{x\to 1}(x+1) = 2\) — сокращаем.</p>
<p>3)&nbsp; \(\lim_{x\to 0}\dfrac{\sin x}{x} = 1\) — предел существует.</p>"""
                ),
                TheoryStep(
                    title = "Первый замечательный предел",
                    formula = """\[\lim_{x \to 0} \frac{\sin x}{x} = 1\]""",
                    note = """Аналогично: \(\lim\frac{\tan x}{x}=1\), \(\lim\frac{\arcsin x}{x}=1\) при x→0. Пример: \(\lim\frac{\sin 3x}{x}=3\)""",
                    detail = """<p>Когда видишь \(\frac{\sin(\ldots)}{\text{то же самое}}\) и аргумент стремится к нулю — результат 1. Аргумент синуса и знаменатель должны быть <strong>одинаковыми</strong>.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(\lim_{x\to 0}\dfrac{\sin x}{x}=1\)</p>
<p>2)&nbsp; \(\lim_{x\to 0}\dfrac{\sin 3x}{x} = \dfrac{\sin 3x}{3x}\cdot 3 \to 3\)</p>
<p>3)&nbsp; \(\lim_{x\to 0}\dfrac{\sin 5x}{\sin 3x} = \dfrac{5}{3}\)</p>"""
                ),
                TheoryStep(
                    title = "Второй замечательный предел",
                    formula = """\[\lim_{x \to \infty}\left(1 + \frac{1}{x}\right)^x = e \qquad \lim_{x\to 0}(1+x)^{1/x} = e\]""",
                    note = """Число e ≈ 2.71828. Пример: \(\lim\left(1+\frac{2}{n}\right)^n = e^2\)""",
                    detail = """<p>Выражение вида \((1+\text{маленькое})^{1/\text{то же самое}}\) → результат \(e\). Если показатель в \(k\) раз больше — \(e^k\).</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(\lim_{n\to\infty}\left(1+\dfrac{1}{n}\right)^n = e\) — определение числа \(e\).</p>
<p>2)&nbsp; \(\lim_{n\to\infty}\left(1+\dfrac{2}{n}\right)^n = e^2\)</p>
<p>3)&nbsp; \(\lim_{x\to 0}(1+x)^{1/x}=e\)</p>"""
                ),
                TheoryStep(
                    title = "Правило Лопиталя",
                    formula = """\[\lim_{x\to a}\frac{f(x)}{g(x)} = \lim_{x\to a}\frac{f'(x)}{g'(x)} \quad \left[\frac{0}{0}\text{ или }\frac{\infty}{\infty}\right]\]""",
                    note = """Применяется при неопределённостях 0/0 или ∞/∞. Пример: \(\lim_{x\to 0}\frac{\sin x}{x}=\lim_{x\to 0}\frac{\cos x}{1}=1\)""",
                    detail = """<p>При неопределённости \(\frac{0}{0}\) или \(\frac{\infty}{\infty}\): дифференцируй числитель и знаменатель <strong>по отдельности</strong>, бери предел снова.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(\lim_{x\to 0}\dfrac{\sin x}{x}\): тип \(\frac{0}{0}\) → \(\dfrac{\cos x}{1}\to 1\)</p>
<p>2)&nbsp; \(\lim_{x\to\infty}\dfrac{x}{e^x}\): тип \(\frac{\infty}{\infty}\) → \(\dfrac{1}{e^x}\to 0\)</p>
<p>3)&nbsp; \(\lim_{x\to 0}\dfrac{e^x-1}{x}\): → \(\dfrac{e^x}{1}\to 1\)</p>"""
                ),
                TheoryStep(
                    title = "Эквивалентные бесконечно малые",
                    formula = """\[x\to 0:\quad \sin x \sim x,\quad \tan x \sim x,\quad \ln(1+x)\sim x,\quad e^x-1\sim x\]""",
                    note = """При x→0 можно заменять в произведениях и частных. Пример: \(\lim\frac{\ln(1+x)}{\sin x}=1\)""",
                    detail = """<p>При \(x\to 0\) эти функции ведут себя "как \(x\)". В пределах (в произведениях и частных) их можно заменять на \(x\). Но <strong>не в суммах</strong>!</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(\lim_{x\to 0}\dfrac{\ln(1+x)}{\sin x}= \dfrac{x}{x}=1\)</p>
<p>2)&nbsp; \(\lim_{x\to 0}\dfrac{e^x-1}{x}=1\)</p>
<p>3)&nbsp; \(\lim_{x\to 0}\dfrac{\tan 2x}{\sin 3x}=\dfrac{2}{3}\)</p>"""
                ),
                TheoryStep(
                    title = "Непрерывность функции",
                    formula = """\[f \text{ непрерывна в } x_0 \Leftrightarrow \lim_{x\to x_0}f(x) = f(x_0)\]""",
                    note = """Три условия: f(x₀) определена, предел существует, предел равен значению функции.""",
                    detail = """<p>Непрерывность — нет "разрывов". Три условия: функция определена, предел существует, предел = значению. Полиномы, \(e^x\), \(\sin x\), \(\cos x\) непрерывны везде.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(f(x)=x^2\) непрерывна везде.</p>
<p>2)&nbsp; \(f(x)=\dfrac{1}{x}\) — разрыв при \(x=0\).</p>
<p>3)&nbsp; \(f(x)=\dfrac{\sin x}{x}\) при \(x\neq0\) непрерывна; при \(f(0)=1\) — тоже непрерывна.</p>"""
                ),
                TheoryStep(
                    title = "Односторонние пределы",
                    formula = """\[\lim_{x\to a^-}f(x) = L^- \qquad \lim_{x\to a^+}f(x) = L^+\]""",
                    note = """Предел существует ⟺ L⁻ = L⁺. Пример: у \(|x|/x\) при x→0: L⁻ = −1, L⁺ = 1 → предела нет.""",
                    detail = """<p>Двусторонний предел существует только когда оба односторонних существуют <strong>и равны</strong>. Если разные — предела нет.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(f(x)=\dfrac{|x|}{x}\): при \(x\to 0^-\to -1\); \(x\to 0^+\to +1\). Разные → предела нет.</p>
<p>2)&nbsp; \(f(x)=\dfrac{1}{x-1}\): при \(x\to 1^-\to -\infty\); \(x\to 1^+\to +\infty\).</p>"""
                ),
                TheoryStep(
                    title = "Теорема о сжатой переменной",
                    formula = """\[g(x) \le f(x) \le h(x), \quad \lim g = \lim h = L \implies \lim f = L\]""",
                    note = """Если функция зажата между двумя со одним пределом — она тоже стремится к нему. Пример: \(\lim x\sin\frac{1}{x}=0\)""",
                    detail = """<p>Используется, когда напрямую вычислить предел трудно. Находим два "зажима" и оба стремятся к одному числу — наша функция тоже.</p>
<p>Чаще всего: ограниченная функция (\(\sin\), \(\cos\)) × что-то, стремящееся к нулю.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(\lim_{x\to 0}x\sin\dfrac{1}{x}\): \(-|x|\le x\sin\dfrac{1}{x}\le|x|\to 0\)</p>
<p>2)&nbsp; \(\lim_{n\to\infty}\dfrac{\sin n}{n}=0\)</p>
<p>3)&nbsp; \(\lim_{x\to 0}x^2\cos\dfrac{1}{x}=0\)</p>"""
                ),
                TheoryStep(
                    title = "Бесконечно малые и большие",
                    formula = """\[\alpha(x)\to 0, \; \alpha = o(\beta) \iff \lim\frac{\alpha}{\beta}=0, \quad \alpha \sim \beta \iff \lim\frac{\alpha}{\beta}=1\]""",
                    note = """Порядок малости: \(\alpha = O(x^n)\). Замена эквивалентных бесконечно малых ускоряет вычисления.""",
                    detail = """<p><strong>Символ \(o\)</strong>: \(\alpha=o(\beta)\) — \(\alpha\) убывает быстрее \(\beta\). \(\sin x = x + o(x)\) при \(x\to 0\).</p>
<p><strong>Символ \(O\)</strong>: \(\alpha=O(\beta)\) — \(|\alpha|\le C|\beta|\). Оценка роста.</p>
<p><strong>Эквивалентность \(\sim\)</strong>: при \(x\to 0\): \(\sin x\sim x\), \(\ln(1+x)\sim x\), \(e^x-1\sim x\), \(1-\cos x\sim x^2/2\).</p>"""
                ),
                TheoryStep(
                    title = "Определение предела по Коши (ε-δ)",
                    formula = """\[\forall\varepsilon>0\;\exists\delta>0: 0<|x-a|<\delta\Rightarrow|f(x)-L|<\varepsilon\]""",
                    note = """Строгое формальное определение предела. Для любой точности ε найдём окрестность δ.""",
                    detail = """<p>Предел — строгий способ сказать «насколько близко к \(L\) можно гарантированно подойти».</p>
<p>На практике: для любого «вызова» \(\varepsilon\) (насколько близко хотим к \(L\)) мы должны найти \(\delta\) (насколько близко нужно взять \(x\) к \(a\)).</p>
<p><strong>Пример:</strong> Докажем \(\lim_{x\to 2} (3x-1)=5\). \(|3x-1-5|=3|x-2|<\varepsilon\) при \(\delta=\varepsilon/3\).</p>"""
                )
            )
        ),

        // ─────────────────────────── ДУ ───────────────────────────
        "ode" to TheorySubject(
            key = "ode", title = "Дифференциальные уравнения", icon = "y'", accentColorHex = "#f97316",
            steps = listOf(
                TheoryStep(
                    title = "Прямое интегрирование",
                    formula = """\[y' = f(x) \implies y = \int f(x)\,dx + C\]""",
                    note = """Простейший случай: правая часть зависит только от x. Пример: \(y'=3x^2 \implies y=x^3+C\)""",
                    detail = """<p>Самый простой тип ДУ: производная равна выражению от \(x\). Решение — просто проинтегрировать. Константа \(C\) отражает семейство решений.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(y'=3x^2\) → \(y=x^3+C\)</p>
<p>2)&nbsp; \(y'=\cos x\) → \(y=\sin x+C\)</p>
<p>3)&nbsp; \(y'=e^x+1\) → \(y=e^x+x+C\)</p>"""
                ),
                TheoryStep(
                    title = "Экспоненциальный рост и убывание",
                    formula = """\[y' = ky \implies y = Ce^{kx}\]""",
                    note = """k > 0 — рост, k < 0 — убывание. Пример: \(y'=2y,\,y(0)=3 \implies y=3e^{2x}\)""",
                    detail = """<p>Встречается везде: рост бактерий, радиоактивный распад, проценты. Смысл: <em>скорость изменения пропорциональна текущему значению</em>.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(y'=2y,\ y(0)=3\): \(y=Ce^{2x}\); из \(y(0)=C=3\) → \(y=3e^{2x}\)</p>
<p>2)&nbsp; \(y'=-y\): \(y=Ce^{-x}\) (затухание)</p>
<p>3)&nbsp; \(y'=0{,}05y,\ y(0)=1000\): \(y=1000e^{0{,}05x}\)</p>"""
                ),
                TheoryStep(
                    title = "Разделение переменных",
                    formula = """\[\frac{dy}{dx} = f(x)\cdot g(y) \implies \int\frac{dy}{g(y)} = \int f(x)\,dx + C\]""",
                    note = """Переносим всё с y влево, всё с x — вправо, затем интегрируем обе части.""",
                    detail = """<p>Правую часть можно записать как произведение функций от \(x\) и \(y\) — переносим, интегрируем.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(y'=xy\): \(\dfrac{dy}{y}=x\,dx\) → \(y=Ae^{x^2/2}\)</p>
<p>2)&nbsp; \(y'=y^2\): \(\dfrac{dy}{y^2}=dx\) → \(y=-\dfrac{1}{x+C}\)</p>
<p>3)&nbsp; \(y'=\dfrac{x}{y}\): \(y\,dy=x\,dx\) → \(y^2-x^2=C\) (гиперболы)</p>"""
                ),
                TheoryStep(
                    title = "Линейное ДУ первого порядка",
                    formula = """\[y' + P(x)\,y = Q(x), \quad \mu = e^{\int P(x)\,dx}\]""",
                    note = """Решается умножением на интегрирующий множитель μ. Тогда (μy)′ = μQ(x).""",
                    detail = """<p>Метод: находим <strong>интегрирующий множитель</strong> \(\mu=e^{\int P(x)\,dx}\) и умножаем. Левая часть становится производной \((\mu y)'\).</p>
<p><strong>Шаги:</strong> найди \(\mu\); умножь на \(\mu\); интегрируй обе части.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(y'+y=e^x\): \(\mu=e^x\); \((e^x y)'=e^{2x}\) → \(y=\dfrac{e^x}{2}+Ce^{-x}\)</p>
<p>2)&nbsp; \(y'-2y=1\): \(P=-2\), \(\mu=e^{-2x}\) → \(y=-\dfrac{1}{2}+Ce^{2x}\)</p>"""
                ),
                TheoryStep(
                    title = "Задача Коши",
                    formula = """\[y' = f(x,\,y), \quad y(x_0) = y_0\]""",
                    note = """Начальное условие выделяет единственное решение. Пример: \(y'=y,\,y(0)=1 \implies y=e^x\)""",
                    detail = """<p>Общее решение содержит произвольную константу \(C\). Задача Коши фиксирует одну конкретную кривую из семейства.</p>
<p><strong>Алгоритм:</strong> найди общее решение, подставь начальное условие, вычисли \(C\).</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(y'=2x,\ y(0)=1\): \(y=x^2+C\); \(C=1\) → \(y=x^2+1\)</p>
<p>2)&nbsp; \(y'=y,\ y(0)=5\): \(y=Ce^x\); \(C=5\) → \(y=5e^x\)</p>"""
                ),
                TheoryStep(
                    title = "Однородные уравнения",
                    formula = """\[y' = f\!\left(\frac{y}{x}\right) \xrightarrow{t=y/x} \text{разделение переменных}\]""",
                    note = """Замена t = y/x превращает однородное ДУ в уравнение с разделяемыми переменными.""",
                    detail = """<p>Правая часть зависит только от \(y/x\). Замена \(t=y/x\) (т.е. \(y=tx\)): \(y'=t+xt'\). Разделяем переменные, интегрируем.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(y'=\dfrac{y}{x}\): \(t+xt'=t\) → \(t=C\) → \(y=Cx\)</p>
<p>2)&nbsp; \(y'=\dfrac{y}{x}+1\): \(xt'=1\) → \(t=\ln|x|+C\) → \(y=x(\ln|x|+C)\)</p>"""
                ),
                TheoryStep(
                    title = "ДУ второго порядка с пост. коэфф.",
                    formula = """\[y'' + py' + qy = 0, \quad k^2 + pk + q = 0\]""",
                    note = """Составляем характеристическое уравнение. Корни k₁, k₂ определяют вид решения.""",
                    detail = """<p>Ищем решение в виде \(y=e^{kx}\). Подставляем, выносим \(e^{kx}\), получаем <strong>характеристическое уравнение</strong> — обычное квадратное.</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(y''-3y'+2y=0\): \(k^2-3k+2=0\) → \(k_1=1,\ k_2=2\)</p>
<p>2)&nbsp; \(y''+4y'+4y=0\): \((k+2)^2=0\) → \(k=-2\) (кратный)</p>
<p>3)&nbsp; \(y''+y=0\): \(k^2+1=0\) → \(k=\pm i\)</p>"""
                ),
                TheoryStep(
                    title = "Типы решений по корням",
                    formula = """\[k_1 \neq k_2 \in \mathbb{R}:\; C_1e^{k_1 x}+C_2e^{k_2 x} \quad k_{1,2}=\alpha\pm\beta i:\; e^{\alpha x}(C_1\cos\beta x+C_2\sin\beta x)\]""",
                    note = """Три случая: различные вещественные, кратный корень, комплексные корни.""",
                    detail = """<p><strong>Три случая:</strong></p>
<p>1)&nbsp; \(k_1\neq k_2\) (вещественные): \(y=C_1e^{k_1 x}+C_2e^{k_2 x}\)</p>
<p>2)&nbsp; \(k_1=k_2\) (кратный корень): \(y=(C_1+C_2 x)e^{kx}\) — к экспоненте домножается \(x\).</p>
<p>3)&nbsp; \(k=\alpha\pm\beta i\) (комплексные): \(y=e^{\alpha x}(C_1\cos\beta x+C_2\sin\beta x)\)</p>
<p><strong>Примеры:</strong></p>
<p>1)&nbsp; \(k_1=1,\ k_2=2\): \(y=C_1e^x+C_2e^{2x}\)</p>
<p>2)&nbsp; \(k=-2\) (кратный): \(y=(C_1+C_2 x)e^{-2x}\)</p>
<p>3)&nbsp; \(k=\pm 3i\): \(y=C_1\cos 3x+C_2\sin 3x\)</p>"""
                ),
                TheoryStep(
                    title = "Уравнение Бернулли",
                    formula = """\[y' + P(x)\,y = Q(x)\,y^n \xrightarrow{z=y^{1-n}} \text{линейное ОДУ}\]""",
                    note = """Замена \(z=y^{1-n}\) превращает уравнение Бернулли в линейное. При \(n=0\) — линейное, при \(n=1\) — экспоненциальное.""",
                    detail = """<p>Уравнение Бернулли — нелинейное, но замена \(z=y^{1-n}\) линеаризует его.</p>
<p><strong>Алгоритм:</strong> делим на \(y^n\); вводим \(z=y^{1-n}\); дифференцируем; решаем линейное ОДУ относительно \(z\).</p>
<p><strong>Пример:</strong> \(y' - y = y^2\): \(n=2\), \(z=y^{-1}\), \(-z'-(-z)=1\) → линейное.</p>"""
                ),
                TheoryStep(
                    title = "Точные уравнения",
                    formula = """\[M(x,y)\,dx + N(x,y)\,dy = 0, \quad \frac{\partial M}{\partial y} = \frac{\partial N}{\partial x}\]""",
                    note = """Уравнение точное, если ∂M/∂y = ∂N/∂x. Тогда существует функция F(x,y): dF = Mdx + Ndy.""",
                    detail = """<p><strong>Признак точности:</strong> \(\partial M/\partial y = \partial N/\partial x\).</p>
<p><strong>Решение:</strong> находим \(F\) из \(F_x=M\) интегрированием по \(x\), уточняем из \(F_y=N\). Ответ: \(F(x,y)=C\).</p>
<p><strong>Пример:</strong> \((2x+y)\,dx+(x+2y)\,dy=0\): \(M_y=1=N_x\) — точное. \(F=x^2+xy+y^2=C\).</p>"""
                )
            )
        ),

        // ─────────────────────────── ВЕРОЯТНОСТЬ ───────────────────────────
        "probability" to TheorySubject(
            key = "probability", title = "Теория вероятностей", icon = "P", accentColorHex = "#38bdf8",
            steps = listOf(
                TheoryStep(
                    title = "Случайные события",
                    formula = """\[\Omega = \{\omega_1, \omega_2, \ldots, \omega_n\}, \quad A \subseteq \Omega\]""",
                    note = """Эксперимент → Ω (пространство исходов). Событие A — подмножество Ω. Достоверное: A=Ω; Невозможное: A=∅.""",
                    detail = """<p><strong>Случайный эксперимент</strong> — опыт, результат которого нельзя предсказать заранее. Пример: бросок кубика.</p>
<p><strong>Элементарный исход</strong> ω — неделимый результат. Для кубика: ω ∈ {1,2,3,4,5,6}.</p>
<p><strong>Событие A</strong> — набор исходов. «Выпало чётное» = {2,4,6} ⊆ Ω.</p>
<p><strong>Типы:</strong> Достоверное (A=Ω), невозможное (A=∅), противоположное (Ā = Ω∖A).</p>"""
                ),
                TheoryStep(
                    title = "Классическая вероятность",
                    formula = """\[P(A) = \frac{m}{n}\]""",
                    note = """n — число равновозможных исходов, m — число благоприятных. Всегда 0 ≤ P(A) ≤ 1.""",
                    detail = """<p>Применима, когда все исходы <strong>равновозможны</strong>.</p>
<p><strong>Пример 1.</strong> Кубик: P(чётное) = 3/6 = 0,5.</p>
<p><strong>Пример 2.</strong> Урна: 5 красных и 3 белых. P(красный) = 5/8.</p>
<p><strong>Пример 3.</strong> Два кубика: P(сумма=7) = 6/36 = 1/6.</p>"""
                ),
                TheoryStep(
                    title = "Комбинаторика",
                    formula = """\[C_n^k = \binom{n}{k} = \frac{n!}{k!(n-k)!}, \quad A_n^k = \frac{n!}{(n-k)!}, \quad P_n = n!\]""",
                    note = """C — выбор без учёта порядка; A — с учётом; P — перестановки n элементов.""",
                    detail = """<p><strong>Сочетания</strong> C(n,k): выбрать k из n, порядок не важен. C(5,2)=10.</p>
<p><strong>Размещения</strong> A(n,k): выбрать k из n, порядок важен. A(5,2)=20.</p>
<p><strong>Перестановки</strong> P(n)=n!: все способы расставить n объектов. P(4)=24.</p>
<p><strong>Пример.</strong> Лотерея 6 из 36: C(36,6) = 1 947 792 комбинаций.</p>"""
                ),
                TheoryStep(
                    title = "Алгебра событий",
                    formula = """\[P(A \cup B) = P(A) + P(B) - P(A \cap B)\]""",
                    note = """Для несовместных (A∩B=∅): P(A∪B)=P(A)+P(B). Дополнение: P(Ā)=1−P(A).""",
                    detail = """<p><strong>Сумма A∪B</strong> — хотя бы одно из A, B. <strong>Произведение A∩B</strong> — оба.</p>
<p><strong>Формула включений-исключений</strong> убирает двойной счёт пересечения.</p>
<p><strong>Пример.</strong> Карта: P(туз)=4/52, P(червы)=13/52, P(туз червей)=1/52. P(туз или червы) = 16/52 ≈ 0,308.</p>"""
                ),
                TheoryStep(
                    title = "Условная вероятность",
                    formula = """\[P(A|B) = \frac{P(A \cap B)}{P(B)}, \quad P(B) > 0\]""",
                    note = """Вероятность A при условии, что B уже произошло. Пространство «сужается» до B.""",
                    detail = """<p>Новая информация (событие B произошло) сужает пространство до B.</p>
<p><strong>Пример 1.</strong> Кубик: P(>4 | чётное) = P({6})/P({2,4,6}) = (1/6)/(3/6) = 1/3.</p>
<p><strong>Пример 2.</strong> 10 деталей, 3 бракованные. Берём 2. P(2-я брак | 1-я брак) = 2/9.</p>"""
                ),
                TheoryStep(
                    title = "Независимые события",
                    formula = """\[P(A \cap B) = P(A) \cdot P(B) \quad \text{(если A и B независимы)}\]""",
                    note = """Независимы, если P(A|B)=P(A). Знание о B не меняет P(A).""",
                    detail = """<p>Независимость — свойство, а не очевидный факт. Всегда проверяйте!</p>
<p><strong>Пример 1.</strong> Два броска монеты: P(ОО) = 0,5 × 0,5 = 0,25.</p>
<p><strong>Пример 2.</strong> 3 устройства, надёжность 0,9: P(все работают) = 0,9³ = 0,729.</p>
<p><strong>Пример 3.</strong> P(хотя бы одно откажет) = 1 − 0,729 = 0,271.</p>"""
                ),
                TheoryStep(
                    title = "Формула полной вероятности",
                    formula = """\[P(A) = \sum_{i=1}^{n} P(H_i) \cdot P(A|H_i)\]""",
                    note = """H₁,…,Hₙ — полная группа гипотез (∑P(Hᵢ)=1).""",
                    detail = """<p>Когда напрямую вычислить P(A) сложно, делим пространство на гипотезы (причины).</p>
<p><strong>Пример.</strong> Завод: цех 1 — 50% с 2% брака, цех 2 — 30% с 3%, цех 3 — 20% с 5%.</p>
<p>P(брак) = 0,5×0,02 + 0,3×0,03 + 0,2×0,05 = 0,029 = 2,9%.</p>"""
                ),
                TheoryStep(
                    title = "Формула Байеса",
                    formula = """\[P(H_i|A) = \frac{P(H_i) \cdot P(A|H_i)}{\displaystyle\sum_{j=1}^{n} P(H_j) \cdot P(A|H_j)}\]""",
                    note = """Обновляет вероятность гипотезы после наблюдения A (апостериорная вероятность).""",
                    detail = """<p>Байес позволяет "перевернуть" условную вероятность: знаем P(A|Hᵢ), находим P(Hᵢ|A).</p>
<p><strong>Пример.</strong> Из задачи про завод: случайная деталь оказалась бракованной. Из какого цеха?</p>
<p>P(H₁|брак) ≈ 0,345; P(H₂|брак) ≈ 0,310; P(H₃|брак) ≈ 0,345.</p>"""
                ),
                TheoryStep(
                    title = "Формула Бернулли",
                    formula = """\[P_n(k) = C_n^k \, p^k q^{n-k}, \quad q = 1 - p\]""",
                    note = """Вероятность ровно k успехов в n независимых испытаниях с вероятностью успеха p.""",
                    detail = """<p>Условия: независимые испытания, вероятность p одинакова везде.</p>
<p><strong>Пример 1.</strong> Монета 5 раз: P(3 орла) = C(5,3)×0,5³×0,5² = 10×0,03125 = 0,3125.</p>
<p><strong>Пример 2.</strong> Стрелок p=0,8, 4 выстрела: P(3 попадания) = C(4,3)×0,8³×0,2 ≈ 0,410.</p>"""
                ),
                TheoryStep(
                    title = "Математическое ожидание и дисперсия",
                    formula = """\[M(X) = \sum_{i} x_i \cdot p_i, \quad D(X) = M(X^2) - [M(X)]^2\]""",
                    note = """M(X) — взвешенное среднее. D(X) — дисперсия. σ = √D(X) — стандартное отклонение.""",
                    detail = """<p><strong>Закон распределения</strong> — таблица {xᵢ, pᵢ}. Сумма всех pᵢ = 1.</p>
<p><strong>Пример.</strong> Кубик: M(X) = (1+2+3+4+5+6)/6 = 3,5. D(X) = 91/6 − 12,25 ≈ 2,917.</p>
<p><strong>Свойства:</strong> M(aX+b) = aM(X)+b; D(aX) = a²D(X).</p>"""
                ),
                TheoryStep(
                    title = "Нормальное распределение",
                    formula = """\[f(x) = \frac{1}{\sigma\sqrt{2\pi}}\,e^{-\frac{(x-\mu)^2}{2\sigma^2}}, \quad P(a<X<b) = \Phi\!\left(\frac{b-\mu}{\sigma}\right) - \Phi\!\left(\frac{a-\mu}{\sigma}\right)\]""",
                    note = """Правило трёх сигм: P(|X−μ|<3σ) ≈ 0,997. Стандартизация: Z = (X−μ)/σ ~ N(0,1).""",
                    detail = """<p>Нормальное N(μ,σ²) — важнейшее непрерывное распределение. Встречается везде (измерения, ошибки).</p>
<p><strong>Правило σ:</strong> P(|X−μ|<σ)≈0,683; P(|X−μ|<2σ)≈0,954; P(|X−μ|<3σ)≈0,997.</p>
<p><strong>Пример.</strong> Рост студентов: μ=175, σ=8. P(170<X<185) = Φ(1,25)−Φ(−0,625) ≈ 0,628.</p>"""
                ),
                TheoryStep(
                    title = "Распределение Пуассона",
                    formula = """\[P(X=k) = \frac{\lambda^k e^{-\lambda}}{k!}, \quad M(X)=D(X)=\lambda\]""",
                    note = """Редкие события за фиксированный промежуток времени. λ — среднее число событий.""",
                    detail = """<p>Пуассоновское распределение — предел биномиального при \(n\to\infty\), \(p\to 0\), \(np=\lambda=\text{const}\).</p>
<p><strong>Применение:</strong> число звонков в час, число поломок в день, число мутаций в гене.</p>
<p><strong>Пример.</strong> В среднем 3 опечатки на страницу (\(\lambda=3\)). P(X=0) = e^{-3} ≈ 0,05; P(X=3) ≈ 0,224.</p>"""
                ),
                TheoryStep(
                    title = "Биномиальное распределение",
                    formula = """\[X\sim B(n,p),\quad M(X)=np,\quad D(X)=npq,\quad q=1-p\]""",
                    note = """n независимых испытаний, вероятность успеха p. Среднее np, дисперсия npq.""",
                    detail = """<p>Биномиальное B(n,p): число успехов в n испытаниях Бернулли.</p>
<p><strong>Параметры:</strong> \(M(X)=np\), \(D(X)=npq\), \(\sigma=\sqrt{npq}\).</p>
<p><strong>Пример.</strong> Монета 100 бросков, p=0.5. M(X)=50, D(X)=25, σ=5. P(X=k) — нормально при больших n (ЦПТ).</p>"""
                ),
                TheoryStep(
                    title = "Числовые характеристики СВ",
                    formula = """\[\sigma(X) = \sqrt{D(X)},\quad\text{Мода, Медиана, Квантили}\]""",
                    note = """σ — стандартное отклонение (в тех же единицах что X). Мода — наиболее вероятное значение.""",
                    detail = """<p><strong>Мода</strong> — значение с наибольшей вероятностью. <strong>Медиана</strong> Me: P(X≤Me)=0,5.</p>
<p><strong>Момент k-го порядка</strong>: \(m_k=M[X^k]\). Центральный: \(\mu_k=M[(X-M[X])^k]\).</p>
<p><strong>Связь</strong>: \(D(X)=\mu_2\), асимметрия \(=\mu_3/\sigma^3\), эксцесс \(=\mu_4/\sigma^4-3\).</p>"""
                ),
                TheoryStep(
                    title = "Геометрическое и равномерное распределения",
                    formula = """\[X\sim\text{Геом}(p):\; P(X=k)=q^{k-1}p;\quad X\sim U(a,b):\; f(x)=\frac{1}{b-a}\]""",
                    note = """Геометрическое: ожидание первого успеха. Равномерное: все точки на [a,b] равновероятны.""",
                    detail = """<p><strong>Геометрическое</strong>: число испытаний до первого успеха. \(M(X)=1/p\), \(D(X)=q/p^2\).</p>
<p><strong>Равномерное</strong> U(a,b): \(M(X)=(a+b)/2\), \(D(X)=(b-a)^2/12\).</p>
<p><strong>Пример.</strong> Кидаем кубик до выпадения 6 (p=1/6). Среднее ожидание: 6 бросков.</p>"""
                )
            )
        ),

        // ─────────────────────────── ЛИНЕЙНАЯ АЛГЕБРА ───────────────────────────
        "linalg" to TheorySubject(
            key = "linalg", title = "Линейная алгебра", icon = "Ax", accentColorHex = "#6366f1",
            steps = listOf(
                TheoryStep(
                    title = "Матрицы: основные операции",
                    formula = """\[(A+B)_{ij} = a_{ij}+b_{ij}, \quad (kA)_{ij}=k\,a_{ij}, \quad (A^T)_{ij}=a_{ji}\]""",
                    note = """Сложение — поэлементное. Транспонирование — строки становятся столбцами.""",
                    detail = """<p><strong>Матрица</strong> \(m\times n\) — таблица чисел.</p>
<p><strong>Сложение:</strong> поэлементно, одинакового размера. <strong>Умножение на скаляр:</strong> каждый элемент × k.</p>
<p><strong>Транспонирование:</strong> строки и столбцы меняются. \((A^T)^T=A\), \((AB)^T=B^TA^T\).</p>
<p><strong>Специальные:</strong> нулевая, единичная \(E\) (диагональ=1), симметричная \(A=A^T\).</p>"""
                ),
                TheoryStep(
                    title = "Умножение матриц",
                    formula = """\[(AB)_{ij} = \sum_{k=1}^{n} a_{ik}\,b_{kj}\]""",
                    note = """A (m×n) × B (n×p) → результат m×p. Строка на столбец. Пример: \(\begin{pmatrix}1&2\\3&4\end{pmatrix}\begin{pmatrix}5&6\\7&8\end{pmatrix}=\begin{pmatrix}19&22\\43&50\end{pmatrix}\)""",
                    detail = """<p>Элемент \((i,j)\) произведения \(AB\) = скалярное произведение \(i\)-й строки \(A\) и \(j\)-го столбца \(B\).</p>
<p><strong>Условие:</strong> число столбцов A = числу строк B.</p>
<p><strong>Свойства:</strong> ассоциативность \((AB)C=A(BC)\); дистрибутивность; <em>не коммутативно</em>: \(AB\neq BA\)!</p>"""
                ),
                TheoryStep(
                    title = "Определитель матрицы",
                    formula = """\[\det\begin{pmatrix}a&b\\c&d\end{pmatrix}=ad-bc, \quad \det(AB)=\det A\cdot\det B\]""",
                    note = """Определитель 3×3 — разложение по первой строке с минорами \(M_{ij}\).""",
                    detail = """<p><strong>2×2:</strong> \(ad-bc\). Главная диагональ минус побочная.</p>
<p><strong>3×3 (правило Саррюса):</strong> сумма главных диагональных произведений минус побочных.</p>
<p><strong>Свойства:</strong> \(\det(A^T)=\det A\); \(\det(AB)=\det A\cdot\det B\); линейная зависимость строк → det=0.</p>
<p><strong>Приложение:</strong> \(\det A\neq 0\) ↔ матрица обратима ↔ \(Ax=b\) имеет единственное решение.</p>"""
                ),
                TheoryStep(
                    title = "Обратная матрица",
                    formula = """\[A^{-1} = \frac{1}{\det A}\begin{pmatrix}d&-b\\-c&a\end{pmatrix}, \quad A\cdot A^{-1}=E\]""",
                    note = """Существует только при \(\det A\neq 0\). Для нахождения в общем случае: метод Гаусса-Жордана \([A|E]\to[E|A^{-1}]\).""",
                    detail = """<p><strong>Обратная</strong> \(A^{-1}\): \(A\cdot A^{-1}=E\). Существует тогда и только тогда, когда \(\det A\neq 0\).</p>
<p><strong>Метод Гаусса-Жордана:</strong> записываем \([A|E]\), элементарными операциями приводим к \([E|A^{-1}]\).</p>
<p><strong>Свойства:</strong> \((AB)^{-1}=B^{-1}A^{-1}\); \((A^T)^{-1}=(A^{-1})^T\).</p>"""
                ),
                TheoryStep(
                    title = "Системы линейных уравнений",
                    formula = """\[Ax=b \xrightarrow{\text{Гаусс}} \text{ступенчатая форма} \rightarrow \text{обратная подстановка}\]""",
                    note = """Теорема Кронекера-Капелли: система совместна ⟺ rank(A)=rank(A|b).""",
                    detail = """<p><strong>Метод Гаусса:</strong> расширенную матрицу \([A|b]\) преобразуем к ступенчатому виду, затем обратная подстановка.</p>
<p><strong>Правило Крамера</strong> (det A ≠ 0): \(x_i = \dfrac{\det A_i}{\det A}\).</p>
<p><strong>Ранг:</strong> максимальный порядок ненулевого минора = число ненулевых строк в ступенчатой форме.</p>"""
                ),
                TheoryStep(
                    title = "Собственные значения и векторы",
                    formula = """\[Av = \lambda v \iff \det(A - \lambda E) = 0\]""",
                    note = """Характеристический многочлен: \(\chi(\lambda)=\det(A-\lambda E)\). Для 2×2: \(\lambda^2-\mathrm{tr}(A)\,\lambda+\det A=0\).""",
                    detail = """<p><strong>Собственный вектор</strong> \(v\neq 0\): матрица лишь растягивает/сжимает его без изменения направления.</p>
<p><strong>Алгоритм:</strong> 1) \(\det(A-\lambda E)=0\) → находим \(\lambda\); 2) для каждого \(\lambda_i\) решаем \((A-\lambda_i E)v=0\).</p>
<p><strong>Для 2×2:</strong> \(\mathrm{tr}(A)=\lambda_1+\lambda_2\), \(\det A=\lambda_1\lambda_2\).</p>"""
                ),
                TheoryStep(
                    title = "Линейные пространства и базис",
                    formula = """\[\dim V = n, \quad v = \sum_{i=1}^n c_i\, e_i, \quad \mathrm{rank}(A)+\dim\ker(A)=n\]""",
                    note = """Базис — максимальная линейно независимая система. Теорема о ранге-нуллитете.""",
                    detail = """<p><strong>Линейная независимость:</strong> из \(c_1 v_1+\ldots+c_k v_k=0\) следует все \(c_i=0\).</p>
<p><strong>Базис:</strong> линейно независимые векторы, порождающие всё пространство.</p>
<p><strong>Ядро:</strong> \(\ker A=\{x:Ax=0\}\) — решения однородной системы.</p>
<p><strong>Теорема:</strong> \(\mathrm{rank}(A)+\dim\ker(A)=n\).</p>"""
                ),
                TheoryStep(
                    title = "Скалярное и векторное произведения",
                    formula = """\[\langle u,v\rangle = \sum_i u_i v_i = |u||v|\cos\theta, \quad u\times v = \det\begin{pmatrix}e_1&e_2&e_3\\u_1&u_2&u_3\\v_1&v_2&v_3\end{pmatrix}\]""",
                    note = """Ортогональны ⟺ \(\langle u,v\rangle=0\). Норма: \(\|v\|=\sqrt{\langle v,v\rangle}\).""",
                    detail = """<p><strong>Скалярное:</strong> \(\langle u,v\rangle = u_1v_1+\ldots+u_nv_n\). Угол: \(\cos\theta = \dfrac{\langle u,v\rangle}{\|u\|\|v\|}\).</p>
<p><strong>Ортогональность:</strong> \(u\perp v\iff\langle u,v\rangle=0\).</p>
<p><strong>Векторное произведение</strong> (в \(\mathbb{R}^3\)): вектор, перпендикулярный обоим, длина \(|u||v|\sin\theta\).</p>
<p><strong>Процесс Грама-Шмидта</strong> строит ортогональный базис из любого базиса.</p>"""
                ),
                TheoryStep(
                    title = "Ранг матрицы. Теорема Кронекера-Капелли",
                    formula = """\[\mathrm{rank}(A) = \mathrm{rank}(A|b) \iff Ax=b \text{ совместна}\]""",
                    note = """Ранг = максимальный порядок ненулевого минора = число ненулевых строк в ступенчатой форме.""",
                    detail = """<p><strong>Ранг</strong> — размер максимального базисного минора. Вычисляется методом Гаусса: считаем ненулевые строки после приведения.</p>
<p><strong>Теорема Кронекера-Капелли:</strong> система \(Ax=b\) совместна тогда и только тогда, когда \(\mathrm{rank}(A)=\mathrm{rank}(A|b)\).</p>
<p>Если \(\mathrm{rank}=n\) (число неизвестных) — единственное решение. Если \(\mathrm{rank}<n\) — бесконечно много (свободные переменные).</p>"""
                ),
                TheoryStep(
                    title = "Диагонализация матриц",
                    formula = """\[A = P\,D\,P^{-1}, \quad D=\mathrm{diag}(\lambda_1,\ldots,\lambda_n)\]""",
                    note = """P — матрица из собственных векторов. Матрица диагонализируема ⟺ имеет n линейно независимых собственных векторов.""",
                    detail = """<p><strong>Диагонализация:</strong> находим собственные значения \(\lambda_i\) и векторы \(v_i\), составляем \(P=[v_1|\ldots|v_n]\).</p>
<p><strong>Приложение степеней:</strong> \(A^k = P\,D^k\,P^{-1}\), \(D^k=\mathrm{diag}(\lambda_1^k,\ldots,\lambda_n^k)\) — вычислять гораздо проще.</p>
<p><strong>Симметричная матрица</strong> всегда диагонализируема над \(\mathbb{R}\), собственные векторы ортогональны.</p>"""
                )
            )
        )
    )
}
