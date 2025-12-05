import re

# Leer la primera línea del archivo
with open("transiciones.txt", "r") as f:
    primera = f.readline()

# Eliminar posibles espacios o salto de línea al inicio/final
linea = primera.strip()

regex = '(T0)(.*?)(T1)(?![01])(.*?)((T2)(.*?)(T3)(.*?)(T4)(.*?)|(T5)(.*?)(T6)(.*?)|(T7)(.*?)(T8)(.*?)(T9)(.*?)(T10)(.*?))(T11)(.*?)'
sub = '\g<2>\g<4>\g<7>\g<9>\g<11>\g<13>\g<15>\g<17>\g<19>\g<21>\g<23>\g<25>'

# Contadores para cada invariante de transición
count_t2 = 0
count_t5 = 0
count_t7 = 0

while True:
    try:

        nueva_linea, count = re.subn(regex, sub, linea)
       
        matches = list(re.finditer(regex, linea))
        if not matches:
            print("No se encontraron más coincidencias")
            break
        for match in matches:
            if match.group(6):
                count_t2 += 1
            if match.group(12):
                count_t5 += 1
            if match.group(18):
                count_t7 += 1
    
    except re.error as e:
        print(f"Error en la regex: {e}")
        break

    print("Iteración reemplazos:", count, "->", nueva_linea)
    if count == 0:
        break
    linea = nueva_linea

print("Resultado final:", linea)
print(f"Veces que aparece invariant T2: {count_t2}")
print(f"Veces que aparece invariant T5: {count_t5}")
print(f"Veces que aparece invariant T7: {count_t7}")
print("Cantidad de invariantes completados:", count_t2 + count_t5 + count_t7)
