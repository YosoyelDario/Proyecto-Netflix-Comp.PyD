# Proyecto-Netflix-Comp.PyD
Proyecto de la asignatura de Computación Paralela y Distribuida.

# COMO EJECUTAR

# PASO 0 COMPILAR LOS JAVAS (HAGANLO CON POWERSHELL)

javac -d build/classes src/compartido/*.java src/servidores/*.java src/cliente/*.java

# Terminal 1: Servidor B (Auth)
java -cp build/classes servidores.ServidorAutenticacion

# Terminal 2: Servidor A (Catálogo)  
java -cp build/classes servidores.ServidorCatalogo

# Terminal 3: Gateway ZUUL
java -cp build/classes servidores.GatewayZuul

# Terminal 4: Servidor Streaming UDP
java -cp build/classes servidores.ServidorStreamingUDP

# Terminal 5: Servidor Subtítulos
java -cp build/classes servidores.ServidorSubtitulos

# Terminal 6: Ejecutar clientes con ip's distintas
java -cp build/classes cliente.ClienteInteractivo localhost 4000 localhost 6000 localhost 7000 127.0.0.3
java -cp build/classes cliente.ClienteInteractivo localhost 4000 localhost 6000 localhost 7000 127.0.0.2
