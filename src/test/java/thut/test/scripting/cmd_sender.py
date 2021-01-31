import socket
import time
import random

HOST = '127.0.0.1'  # Standard loopback interface address (localhost)
PORT = 6666         # Port to listen on (non-privileged ports are > 1023)

n = 0

while True:
    time.sleep(1)
    print(str(30-n))
    n = n + 1
    if n < 30:
        continue
    n = 0
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        try:
            x = random.randint(-10000, 10000)
            z = random.randint(-10000, 10000)
            s.connect((HOST, PORT))
            msg = '/tp @p {} ~ {}'.format(x, z)
            print(msg)
            s.sendall(msg.encode('utf-8'))
            data = s.recv(1024)
            print('Received', repr(data))
            s.close()
        except:
            pass