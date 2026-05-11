# Proyecto-Netflix-Comp.PyD
Proyecto de la asignatura de Computación Paralela y Distribuida.

# COSAS POR MEJORAR:

- MULTI-SERVIDORES (POR EJEMPLO SI SE CAE UNO, QUE PEUDA SEGUIR VIENDO LA PELICULA)

    - Cuando se envian los fragmentos dentro de un envio de una pelica (ej: El Padrino) si al medio del envio de los fragmentos, este se "crashea" y deja de funcionar, este debiese de reconectarse al servidor más "cercano" (En este caso sería como "Para reproducir 1 pelicula se tiene que abrir el servidor A el que envia la pelicula y otro como backUp por si llega a pasar algo al momento de enviar fragmentos).

- SINCRONIZAR PELICULA EN REPRODUCCION Y SUBTITULOS

- HACER QUE SALGA UNA OPCION DE QUE SI EL USUARIO QUIERE SUBTITULO O NO

- SI O SI EL USUARIO TIENE QUE INICIAR SESION ANTES DE ENTRAR A VER LOS OTROS MENUS

- Mann in the middle:

    - Un server ajeno, intentaria ver tu perfil o la pelicula que estas viendo sin tu autorización (esto se arreglaria con el SSLsockets y medidas de seguridad)



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
