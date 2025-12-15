import re

# Leer la primera línea del archivo
with open("transiciones.txt", "r") as f:
    primera = f.readline()

# Eliminar posibles espacios o salto de línea al inicio/final
linea = primera.strip()

regex = '(T0)(.*?)(T1)(?![01])(.*?)((T2)(.*?)(T3)(.*?)(T4)(.*?)|(T5)(.*?)(T6)(.*?)|(T7)(.*?)(T8)(.*?)(T9)(.*?)(T10)(.*?))(T11)(.*?)'
sub = '\g<2>\g<4>\g<7>\g<9>\g<11>\g<13>\g<15>\g<17>\g<19>\g<21>\g<23>\g<25>'

# Contadores para cada invariante de transición
invariante_1 = 0
invariante_2 = 0
invariante_3 = 0

while True:
  
    nueva_linea, count = re.subn(regex, sub, linea)
    
    matches = re.finditer(regex, linea)
    for match in matches:
        if match.group(6):
            invariante_1 += 1
        if match.group(12):
            invariante_2 += 1
        if match.group(18):
            invariante_3 += 1

    print("Iteración reemplazos:", count, "->", nueva_linea)
    if count == 0:
        break
    linea = nueva_linea

print("Resultado final:", linea)
print(f"Veces que se toma invariante 1: {invariante_1}")
print(f"Veces que se toma invariante 2: {invariante_2}")
print(f"Veces que se toma invariante 3: {invariante_3}")
print("Cantidad de invariantes completados:", invariante_1 + invariante_2 + invariante_3)
