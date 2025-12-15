import re

# Leer la primera línea del archivo
with open("transiciones.txt", "r") as f:
    primera = f.readline()

# Eliminar posibles espacios o salto de línea al inicio/final
linea = primera.strip()

reg = '(T10|T4)(((?!T11).)*?)(T6)(.*?)(T11)'
sub = '\g<2>\g<4>'

while True:

        nueva_linea, count = re.subn(reg, sub, linea)
       
        print("Iteración reemplazos:", count)
        if count == 0:
            break
        linea = nueva_linea
