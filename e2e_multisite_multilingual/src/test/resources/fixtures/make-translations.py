#!/usr/bin/env python3
"""Write the content-translation fixture for the Census Household Survey and check it the way
Author's TranslationHandoff import checks a returned file."""
import json, re, sys, pathlib

ES = {
 "surveys|title|Census Household Survey": "Encuesta Censal de Hogares",
 "surveys|description|A demonstration survey about census household data. It uses every question type and every rule the Elicit engine supports, exactly once where practical.":
   "Una encuesta de demostración sobre datos censales de hogares. Usa todos los tipos de pregunta y todas las reglas que admite el motor de Elicit, una vez cada uno cuando es posible.",

 "steps|name|Welcome": "Bienvenida",
 "steps|description|Welcome step": "Paso de bienvenida",
 "steps|name|About You": "Sobre usted",
 "steps|description|About You step": "Paso sobre usted",
 "steps|name|Your Home": "Su vivienda",
 "steps|description|Your Home step": "Paso de su vivienda",
 "steps|name|Household Members": "Integrantes del hogar",
 "steps|description|Household Members step": "Paso de integrantes del hogar",
 "steps|name|{<NAME>|this person}": "{<NAME>|esta persona}",
 "steps|description|{<NAME>|this person} step": "Paso de {<NAME>|esta persona}",
 "steps|name|Finishing Up": "Para terminar",
 "steps|description|Finishing Up step": "Paso para terminar",

 "sections|name|Introduction": "Introducción",
 "sections|description|Introduction section": "Sección de introducción",
 "sections|name|About you": "Sobre usted",
 "sections|description|About you section": "Sección sobre usted",
 "sections|name|Race and language": "Raza e idioma",
 "sections|description|Race and language section": "Sección de raza e idioma",
 "sections|name|Housing": "Vivienda",
 "sections|description|Housing section": "Sección de vivienda",
 "sections|name|Rent details": "Detalles del alquiler",
 "sections|description|Rent details section": "Sección de detalles del alquiler",
 "sections|name|Vehicles": "Vehículos",
 "sections|description|Vehicles section": "Sección de vehículos",
 "sections|name|Vehicle": "Vehículo",
 "sections|description|Vehicle section": "Sección de vehículo",
 "sections|name|Household members": "Integrantes del hogar",
 "sections|description|Household members section": "Sección de integrantes del hogar",
 "sections|name|{<NAME>|this person}": "{<NAME>|esta persona}",
 "sections|description|{<NAME>|this person} section": "Sección de {<NAME>|esta persona}",
 "sections|name|Contact": "Contacto",
 "sections|description|Contact section": "Sección de contacto",
 "sections|name|Anything else": "Algo más",
 "sections|description|Anything else section": "Sección de algo más",

 "questions|text|<h1>Census Household Survey</h1><p>This short survey asks about the people living in your home, the home itself, and how to reach you. It is a demonstration survey: it exercises every question type and every rule the Elicit engine supports.</p><p>Your answers save as you go, so you can stop and come back at any time.</p>":
   "<h1>Encuesta Censal de Hogares</h1><p>Esta breve encuesta pregunta por las personas que viven en su casa, por la vivienda misma y por cómo comunicarnos con usted. Es una encuesta de demostración: ejercita todos los tipos de pregunta y todas las reglas que admite el motor de Elicit.</p><p>Sus respuestas se guardan a medida que avanza, así que puede detenerse y volver en cualquier momento.</p>",
 "questions|short_text|Welcome": "Bienvenida",
 "questions|text|I have read the above and agree to take part in this survey.": "He leído lo anterior y acepto participar en esta encuesta.",
 "questions|short_text|Consent": "Consentimiento",
 "questions|text|What is your full name?": "¿Cuál es su nombre completo?",
 "questions|short_text|Name": "Nombre",
 "questions|validation_text|Please enter your name.": "Por favor, ingrese su nombre.",
 "questions|text|How old are you?": "¿Cuántos años tiene?",
 "questions|short_text|Age": "Edad",
 "questions|validation_text|Please enter an age between 0 and 120.": "Por favor, ingrese una edad entre 0 y 120.",
 "questions|text|What is your gender?": "¿Cuál es su género?",
 "questions|short_text|Gender": "Género",
 "questions|validation_text|Please select a value.": "Por favor, seleccione un valor.",
 "questions|text|What is your marital status?": "¿Cuál es su estado civil?",
 "questions|short_text|Marital status": "Estado civil",
 "questions|placeholder|Select one": "Seleccione una opción",
 "questions|text|Which of the following describe you? Select all that apply.": "¿Cuál de las siguientes opciones lo describe? Seleccione todas las que correspondan.",
 "questions|short_text|Race": "Raza",
 "questions|text|Which other race or origin?": "¿Cuál otra raza u origen?",
 "questions|short_text|Other race": "Otra raza",
 "questions|text|Which languages are spoken in this home?": "¿Qué idiomas se hablan en esta vivienda?",
 "questions|short_text|Languages": "Idiomas",
 "questions|placeholder|Select all that apply": "Seleccione todas las que correspondan",
 "questions|text|Is this home owned or rented?": "¿Esta vivienda es propia o alquilada?",
 "questions|short_text|Tenure": "Tenencia",
 "questions|text|About when did you move into this home?": "¿Aproximadamente cuándo se mudó a esta vivienda?",
 "questions|short_text|Move-in date": "Fecha de mudanza",
 "questions|text|Is any part of your housing cost subsidized?": "¿Alguna parte del costo de su vivienda está subsidiada?",
 "questions|short_text|Subsidized": "Subsidiada",
 "questions|text|About how much is the monthly rent, in US dollars?": "¿Aproximadamente cuánto es el alquiler mensual, en dólares estadounidenses?",
 "questions|short_text|Monthly rent": "Alquiler mensual",
 "questions|validation_text|Please enter an amount between 0 and 20000.": "Por favor, ingrese un monto entre 0 y 20000.",
 "questions|placeholder|0.00": "0.00",
 "questions|text|How many cars, vans or trucks are kept at this home?": "¿Cuántos autos, camionetas o camiones se guardan en esta vivienda?",
 "questions|short_text|Vehicle count": "Cantidad de vehículos",
 "questions|validation_text|Please enter a number between 0 and 10.": "Por favor, ingrese un número entre 0 y 10.",
 "questions|text|What is the make and model of this vehicle?": "¿Cuál es la marca y el modelo de este vehículo?",
 "questions|short_text|Vehicle": "Vehículo",
 "questions|placeholder|For example, Ford F-150": "Por ejemplo, Ford F-150",
 "questions|text|Not counting yourself, how many other people live in this home?": "Sin contarse a usted, ¿cuántas otras personas viven en esta vivienda?",
 "questions|short_text|Household size": "Tamaño del hogar",
 "questions|validation_text|Please enter a number between 0 and 20.": "Por favor, ingrese un número entre 0 y 20.",
 "questions|text|What is the name of person {Q#}?": "¿Cuál es el nombre de la persona {Q#}?",
 "questions|short_text|Person name": "Nombre de la persona",
 "questions|text|How old is {<NAME>|this person}?": "¿Cuántos años tiene {<NAME>|esta persona}?",
 "questions|short_text|Person age": "Edad de la persona",
 "questions|text|What is {<NAME>'s|this person's} gender?": "¿Cuál es el género de {<NAME>|esta persona}?",
 "questions|short_text|Person gender": "Género de la persona",
 "questions|text|What is {<NAME>'s|this person's} relationship to {<PROBAND>|you}?": "¿Cuál es la relación de {<NAME>|esta persona} con {<PROBAND>|usted}?",
 "questions|short_text|Relationship": "Parentesco",
 "questions|text|What email address should we use to confirm your response?": "¿Qué dirección de correo electrónico debemos usar para confirmar su respuesta?",
 "questions|short_text|Email": "Correo electrónico",
 "questions|validation_text|Please enter a valid email address.": "Por favor, ingrese una dirección de correo electrónico válida.",
 "questions|placeholder|you@example.com": "usted@ejemplo.com",
 "questions|text|What time do you usually leave home for work?": "¿A qué hora suele salir de casa para ir al trabajo?",
 "questions|short_text|Departure time": "Hora de salida",
 "questions|text|If we need to follow up, what date and time works best?": "Si necesitamos comunicarnos de nuevo, ¿qué fecha y hora le convienen más?",
 "questions|short_text|Follow-up": "Seguimiento",
 "questions|text|Is there anything else about your household you would like to tell us?": "¿Hay algo más sobre su hogar que quisiera contarnos?",
 "questions|short_text|Comments": "Comentarios",
 "questions|text|<p>Thank you for completing the Census Household Survey.</p><p>Your answers have been saved. You may close this window.</p>":
   "<p>Gracias por completar la Encuesta Censal de Hogares.</p><p>Sus respuestas se han guardado. Puede cerrar esta ventana.</p>",
 "questions|short_text|Thank you": "Gracias",

 "select_items|display_text|Male": "Masculino",
 "select_items|display_text|Female": "Femenino",
 "select_items|display_text|Another gender": "Otro género",
 "select_items|display_text|Prefer not to say": "Prefiero no decirlo",
 "select_items|display_text|Single, never married": "Soltero, nunca casado",
 "select_items|display_text|Married": "Casado",
 "select_items|display_text|Separated": "Separado",
 "select_items|display_text|Divorced": "Divorciado",
 "select_items|display_text|Widowed": "Viudo",
 "select_items|display_text|White": "Blanca",
 "select_items|display_text|Black or African American": "Negra o afroamericana",
 "select_items|display_text|American Indian or Alaska Native": "Indígena americana o nativa de Alaska",
 "select_items|display_text|Asian": "Asiática",
 "select_items|display_text|Native Hawaiian or Other Pacific Islander": "Nativa de Hawái o de otras islas del Pacífico",
 "select_items|display_text|Some other race or origin": "Alguna otra raza u origen",
 "select_items|display_text|English": "Inglés",
 "select_items|display_text|Spanish": "Español",
 "select_items|display_text|Arabic": "Árabe",
 "select_items|display_text|Chinese": "Chino",
 "select_items|display_text|Another language": "Otro idioma",
 "select_items|display_text|Owned by you or someone in this household": "Propia de usted o de alguien de este hogar",
 "select_items|display_text|Rented": "Alquilada",
 "select_items|display_text|Occupied without payment of rent": "Ocupada sin pago de alquiler",
 "select_items|display_text|Spouse or partner": "Cónyuge o pareja",
 "select_items|display_text|Child": "Hijo o hija",
 "select_items|display_text|Parent": "Padre o madre",
 "select_items|display_text|Brother or sister": "Hermano o hermana",
 "select_items|display_text|Roommate or housemate": "Compañero de cuarto o de vivienda",
 "select_items|display_text|Other relative": "Otro familiar",

 # Revision 2: the reworded race question, translated by hand (UC-043 A1 -> step 5).
 "questions|text|What race do you consider yourself to be? Select all that apply.": "¿De qué raza se considera usted? Seleccione todas las que correspondan.",
}

AR = {
 "surveys|title|Census Household Survey": "استبيان تعداد الأسر",
 "surveys|description|A demonstration survey about census household data. It uses every question type and every rule the Elicit engine supports, exactly once where practical.":
   "استبيان توضيحي عن بيانات تعداد الأسر. يستخدم كل أنواع الأسئلة وكل القواعد التي يدعمها محرك Elicit، مرة واحدة لكل منها حيث أمكن.",

 "steps|name|Welcome": "الترحيب",
 "steps|description|Welcome step": "خطوة الترحيب",
 "steps|name|About You": "عنك",
 "steps|description|About You step": "خطوة عنك",
 "steps|name|Your Home": "منزلك",
 "steps|description|Your Home step": "خطوة منزلك",
 "steps|name|Household Members": "أفراد الأسرة",
 "steps|description|Household Members step": "خطوة أفراد الأسرة",
 "steps|name|{<NAME>|this person}": "{<NAME>|هذا الشخص}",
 "steps|description|{<NAME>|this person} step": "خطوة {<NAME>|هذا الشخص}",
 "steps|name|Finishing Up": "الإنهاء",
 "steps|description|Finishing Up step": "خطوة الإنهاء",

 "sections|name|Introduction": "مقدمة",
 "sections|description|Introduction section": "قسم المقدمة",
 "sections|name|About you": "عنك",
 "sections|description|About you section": "قسم عنك",
 "sections|name|Race and language": "العِرق واللغة",
 "sections|description|Race and language section": "قسم العِرق واللغة",
 "sections|name|Housing": "الإسكان",
 "sections|description|Housing section": "قسم الإسكان",
 "sections|name|Rent details": "تفاصيل الإيجار",
 "sections|description|Rent details section": "قسم تفاصيل الإيجار",
 "sections|name|Vehicles": "المركبات",
 "sections|description|Vehicles section": "قسم المركبات",
 "sections|name|Vehicle": "مركبة",
 "sections|description|Vehicle section": "قسم المركبة",
 "sections|name|Household members": "أفراد الأسرة",
 "sections|description|Household members section": "قسم أفراد الأسرة",
 "sections|name|{<NAME>|this person}": "{<NAME>|هذا الشخص}",
 "sections|description|{<NAME>|this person} section": "قسم {<NAME>|هذا الشخص}",
 "sections|name|Contact": "التواصل",
 "sections|description|Contact section": "قسم التواصل",
 "sections|name|Anything else": "أي شيء آخر",
 "sections|description|Anything else section": "قسم أي شيء آخر",

 "questions|text|<h1>Census Household Survey</h1><p>This short survey asks about the people living in your home, the home itself, and how to reach you. It is a demonstration survey: it exercises every question type and every rule the Elicit engine supports.</p><p>Your answers save as you go, so you can stop and come back at any time.</p>":
   "<h1>استبيان تعداد الأسر</h1><p>يسأل هذا الاستبيان القصير عن الأشخاص الذين يعيشون في منزلك، وعن المنزل نفسه، وعن كيفية التواصل معك. وهو استبيان توضيحي: يختبر كل أنواع الأسئلة وكل القواعد التي يدعمها محرك Elicit.</p><p>تُحفظ إجاباتك أثناء تقدمك، فيمكنك التوقف والعودة في أي وقت.</p>",
 "questions|short_text|Welcome": "الترحيب",
 "questions|text|I have read the above and agree to take part in this survey.": "لقد قرأت ما سبق وأوافق على المشاركة في هذا الاستبيان.",
 "questions|short_text|Consent": "الموافقة",
 "questions|text|What is your full name?": "ما اسمك الكامل؟",
 "questions|short_text|Name": "الاسم",
 "questions|validation_text|Please enter your name.": "يرجى إدخال اسمك.",
 "questions|text|How old are you?": "كم عمرك؟",
 "questions|short_text|Age": "العمر",
 "questions|validation_text|Please enter an age between 0 and 120.": "يرجى إدخال عمر بين 0 و120.",
 "questions|text|What is your gender?": "ما جنسك؟",
 "questions|short_text|Gender": "الجنس",
 "questions|validation_text|Please select a value.": "يرجى اختيار قيمة.",
 "questions|text|What is your marital status?": "ما حالتك الاجتماعية؟",
 "questions|short_text|Marital status": "الحالة الاجتماعية",
 "questions|placeholder|Select one": "اختر واحدًا",
 "questions|text|Which of the following describe you? Select all that apply.": "أي مما يلي يصفك؟ اختر كل ما ينطبق.",
 "questions|short_text|Race": "العِرق",
 "questions|text|Which other race or origin?": "أي عِرق أو أصل آخر؟",
 "questions|short_text|Other race": "عِرق آخر",
 "questions|text|Which languages are spoken in this home?": "ما اللغات التي تُتحدث في هذا المنزل؟",
 "questions|short_text|Languages": "اللغات",
 "questions|placeholder|Select all that apply": "اختر كل ما ينطبق",
 "questions|text|Is this home owned or rented?": "هل هذا المنزل مملوك أم مستأجر؟",
 "questions|short_text|Tenure": "الحيازة",
 "questions|text|About when did you move into this home?": "متى انتقلت تقريبًا إلى هذا المنزل؟",
 "questions|short_text|Move-in date": "تاريخ الانتقال",
 "questions|text|Is any part of your housing cost subsidized?": "هل أي جزء من تكلفة إسكانك مدعوم؟",
 "questions|short_text|Subsidized": "مدعوم",
 "questions|text|About how much is the monthly rent, in US dollars?": "كم يبلغ الإيجار الشهري تقريبًا، بالدولار الأمريكي؟",
 "questions|short_text|Monthly rent": "الإيجار الشهري",
 "questions|validation_text|Please enter an amount between 0 and 20000.": "يرجى إدخال مبلغ بين 0 و20000.",
 "questions|placeholder|0.00": "0.00",
 "questions|text|How many cars, vans or trucks are kept at this home?": "كم عدد السيارات أو الشاحنات الصغيرة أو الشاحنات المحفوظة في هذا المنزل؟",
 "questions|short_text|Vehicle count": "عدد المركبات",
 "questions|validation_text|Please enter a number between 0 and 10.": "يرجى إدخال رقم بين 0 و10.",
 "questions|text|What is the make and model of this vehicle?": "ما ماركة هذه المركبة وطرازها؟",
 "questions|short_text|Vehicle": "المركبة",
 "questions|placeholder|For example, Ford F-150": "على سبيل المثال، Ford F-150",
 "questions|text|Not counting yourself, how many other people live in this home?": "دون حسابك، كم عدد الأشخاص الآخرين الذين يعيشون في هذا المنزل؟",
 "questions|short_text|Household size": "حجم الأسرة",
 "questions|validation_text|Please enter a number between 0 and 20.": "يرجى إدخال رقم بين 0 و20.",
 "questions|text|What is the name of person {Q#}?": "ما اسم الشخص {Q#}؟",
 "questions|short_text|Person name": "اسم الشخص",
 "questions|text|How old is {<NAME>|this person}?": "كم عمر {<NAME>|هذا الشخص}؟",
 "questions|short_text|Person age": "عمر الشخص",
 "questions|text|What is {<NAME>'s|this person's} gender?": "ما جنس {<NAME>|هذا الشخص}؟",
 "questions|short_text|Person gender": "جنس الشخص",
 "questions|text|What is {<NAME>'s|this person's} relationship to {<PROBAND>|you}?": "ما صلة {<NAME>|هذا الشخص} بـ {<PROBAND>|أنت}؟",
 "questions|short_text|Relationship": "صلة القرابة",
 "questions|text|What email address should we use to confirm your response?": "ما عنوان البريد الإلكتروني الذي ينبغي أن نستخدمه لتأكيد ردك؟",
 "questions|short_text|Email": "البريد الإلكتروني",
 "questions|validation_text|Please enter a valid email address.": "يرجى إدخال عنوان بريد إلكتروني صالح.",
 "questions|placeholder|you@example.com": "you@example.com",
 "questions|text|What time do you usually leave home for work?": "في أي وقت تغادر المنزل عادةً للعمل؟",
 "questions|short_text|Departure time": "وقت المغادرة",
 "questions|text|If we need to follow up, what date and time works best?": "إذا احتجنا إلى المتابعة، فأي تاريخ ووقت هو الأفضل؟",
 "questions|short_text|Follow-up": "المتابعة",
 "questions|text|Is there anything else about your household you would like to tell us?": "هل هناك أي شيء آخر عن أسرتك تريد إخبارنا به؟",
 "questions|short_text|Comments": "التعليقات",
 "questions|text|<p>Thank you for completing the Census Household Survey.</p><p>Your answers have been saved. You may close this window.</p>":
   "<p>شكرًا لإكمالك استبيان تعداد الأسر.</p><p>تم حفظ إجاباتك. يمكنك إغلاق هذه النافذة.</p>",
 "questions|short_text|Thank you": "شكرًا",

 "select_items|display_text|Male": "ذكر",
 "select_items|display_text|Female": "أنثى",
 "select_items|display_text|Another gender": "جنس آخر",
 "select_items|display_text|Prefer not to say": "أفضل عدم الإفصاح",
 "select_items|display_text|Single, never married": "أعزب، لم يتزوج قط",
 "select_items|display_text|Married": "متزوج",
 "select_items|display_text|Separated": "منفصل",
 "select_items|display_text|Divorced": "مطلق",
 "select_items|display_text|Widowed": "أرمل",
 "select_items|display_text|White": "أبيض",
 "select_items|display_text|Black or African American": "أسود أو أمريكي من أصل أفريقي",
 "select_items|display_text|American Indian or Alaska Native": "هندي أمريكي أو من سكان ألاسكا الأصليين",
 "select_items|display_text|Asian": "آسيوي",
 "select_items|display_text|Native Hawaiian or Other Pacific Islander": "من سكان هاواي الأصليين أو من جزر المحيط الهادئ الأخرى",
 "select_items|display_text|Some other race or origin": "عِرق أو أصل آخر",
 "select_items|display_text|English": "الإنجليزية",
 "select_items|display_text|Spanish": "الإسبانية",
 "select_items|display_text|Arabic": "العربية",
 "select_items|display_text|Chinese": "الصينية",
 "select_items|display_text|Another language": "لغة أخرى",
 "select_items|display_text|Owned by you or someone in this household": "مملوك لك أو لأحد أفراد هذه الأسرة",
 "select_items|display_text|Rented": "مستأجر",
 "select_items|display_text|Occupied without payment of rent": "مشغول دون دفع إيجار",
 "select_items|display_text|Spouse or partner": "الزوج أو الشريك",
 "select_items|display_text|Child": "ابن أو ابنة",
 "select_items|display_text|Parent": "أب أو أم",
 "select_items|display_text|Brother or sister": "أخ أو أخت",
 "select_items|display_text|Roommate or housemate": "رفيق غرفة أو رفيق منزل",
 "select_items|display_text|Other relative": "قريب آخر",

 # Revision 2: the reworded race question, translated by hand (UC-043 A1 -> step 5).
 "questions|text|What race do you consider yourself to be? Select all that apply.": "ما العِرق الذي تعتبر نفسك منه؟ اختر كل ما ينطبق.",
}

# ---- the checks Author's TranslationHandoff.importFile applies ---------------------------------
PLACEHOLDER = re.compile(r"\{([^{}|]+)(?:\|([^{}]*))?\}")
BARE = re.compile(r"^[A-Za-z0-9_]+$")
BUILT_IN = {"Q#", "S#"}
KNOWN_TOKENS = {"name", "proband"}      # the tokens this survey's rules carry
WIDTHS = {"surveys.title": 255, "surveys.description": 2000, "steps.name": 255,
          "steps.description": 255, "sections.name": 255, "sections.description": 255,
          "questions.text": 8000, "questions.short_text": 100, "questions.tool_tip": 255,
          "questions.placeholder": 255, "questions.validation_text": 255,
          "select_items.display_text": 255}

def placeholders(text):
    out = []
    for m in PLACEHOLDER.finditer(text or ""):
        phrase = m.group(1).strip()
        if phrase and phrase not in BUILT_IN:
            out.append((phrase, m.group(2)))
    return out

def tokens_of(text):
    used = {p for p, _ in placeholders(text) if BARE.match(p)}
    for token in KNOWN_TOKENS:
        if any(token in phrase for phrase, _ in placeholders(text)):
            used.add(token)
    return used

def unbalanced(text):
    open_ = 0
    for c in text:
        if c == "{":
            open_ += 1
            if open_ > 1:
                return True
        elif c == "}":
            open_ -= 1
            if open_ < 0:
                return True
    return open_ != 0

def budget(element_type, field, source):
    width = WIDTHS.get(f"{element_type}.{field}", 255)
    n = len(source or "")
    if n == 0:
        return width
    factor = 2.0 if n <= 20 else 1.6 if n <= 50 else 1.4 if n <= 200 else 1.3
    return max(width, -(-int(n * factor * 1000) // 1000) if False else __import__("math").ceil(n * factor))

def check(lang, table, expected_keys):
    problems = []
    option_labels = {}
    for key, translation in table.items():
        element_type, field, source = key.split("|", 2)
        if not translation.strip():
            problems.append(f"{lang}: {key!r} has an empty translation")
            continue
        if len(placeholders(source)) != len(placeholders(translation)):
            problems.append(f"{lang}: {source[:50]!r}: {len(placeholders(source))} placeholder(s) in the source,"
                            f" {len(placeholders(translation))} in the translation")
        if tokens_of(source) != tokens_of(translation):
            problems.append(f"{lang}: {source[:50]!r}: tokens {tokens_of(translation)} != source's {tokens_of(source)}")
        if unbalanced(translation):
            problems.append(f"{lang}: {source[:50]!r}: unbalanced braces")
        b = budget(element_type, field, source)
        if len(translation) > b:
            problems.append(f"{lang}: {source[:50]!r}: {len(translation)} characters over the {b} allowed")
        if element_type == "select_items":
            if translation in option_labels:
                problems.append(f"{lang}: option {source!r} and {option_labels[translation]!r} are both {translation!r}")
            option_labels[translation] = source
    missing = [k for k in expected_keys if k not in table]
    extra = [k for k in table if k not in expected_keys and "What race do you consider" not in k]
    problems += [f"{lang}: no translation for {k!r}" for k in missing]
    problems += [f"{lang}: {k!r} translates nothing in the survey" for k in extra]
    return problems

keys = json.load(open(sys.argv[1]))
problems = check("es-419", ES, keys) + check("ar", AR, keys)
if problems:
    print("\n".join(problems), file=sys.stderr)
    sys.exit(1)

out = {
    "_comment": ("Content translations of samples/census-household-survey.elicit, keyed "
                 "element_type|field|source_text. Filled into the hand-off document Author "
                 "exports (UC-045) and imported back through the Translations page (UC-046); "
                 "see README.md. The last entry of each language is the reworded race question "
                 "of revision 2, translated by hand."),
    "es-419": ES,
    "ar": AR,
}
pathlib.Path(sys.argv[2]).write_text(json.dumps(out, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"wrote {sys.argv[2]}: es-419={len(ES)}, ar={len(AR)} translations, {len(keys)} survey strings covered")
