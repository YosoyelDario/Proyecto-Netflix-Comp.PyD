# Proyecto-Netflix-Comp.PyD
Proyecto de la asignatura de Computación Paralela y Distribuida.

COSAS POR MEJORAR:

- MULTI-SERVIDORES (POR EJEMPLO SI SE CAE UNO, QUE PEUDA SEGUIR VIENDO LA PELICULA)
- SINCRONIZAR PELICULA EN REPRODUCCION Y SUBTITULOS
- HACER QUE SALGA UNA OPCION DE QUE SI EL USUARIO QUIERE SUBTITULO O NO
- SI O SI EL USUARIO TIENE QUE INICIAR SESION ANTES DE ENTRAR A VER LOS OTROS MENUS


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

# Terminal 6: Cliente
java -cp build/classes cliente.ClienteInteractivo
