import pygame
import socket
import threading
import chess

WIDTH, HEIGHT = 900, 600
SQUARE = 75

class ChessClient:
    def __init__(self, host='localhost', port=12345):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.connect((host, port))
        self.board = chess.Board()
        self.color = None
        self.turn = "white"
        self.running = True
        self.chat = []
        self.msg = ""
        threading.Thread(target=self.listen, daemon=True).start()
        self.gui()

    def listen(self):
        while self.running:
            try:
                data = self.sock.recv(1024).decode('utf-8')
                if data.startswith("color:"):
                    self.color = data.split(":")[1]
                elif data.startswith("board:"):
                    self.board.set_fen(data.split(":")[1])
                    self.turn = "white" if self.board.turn else "black"
                elif data.startswith("chat:"):
                    self.chat.append(data[5:])
            except:
                self.running = False

    def send_move(self, move): self.sock.sendall(f"move {move}".encode())
    def send_chat(self, txt):
        if self.color: txt = f"{self.color.capitalize()}: {txt}"
        self.sock.sendall(f"chat {txt}".encode()); self.msg = ""

    def gui(self):
        pygame.init()
        self.screen = pygame.display.set_mode((WIDTH, HEIGHT))
        pygame.display.set_caption("Chess")
        self.font = pygame.font.Font(None, 24)
        self.imgs = {f'{c}{p}': pygame.image.load(f'assets/{c}{p}.png') for c in 'wb' for p in 'pnbrqk'}
        self.sel = None
        self.loop()

    def draw_board(self):
        for r in range(8):
            for c in range(8):
                pygame.draw.rect(self.screen, pygame.Color("white") if (r+c)%2==0 else pygame.Color("gray"),
                                 pygame.Rect(c*SQUARE, r*SQUARE, SQUARE, SQUARE))
                p = self.board.piece_at(chess.square(c, 7-r))
                if p:
                    key = f"{'w' if p.color else 'b'}{p.symbol().lower()}"
                    self.screen.blit(self.imgs[key], (c*SQUARE, r*SQUARE))

    def handle_click(self, pos):
        col, row = pos[0] // SQUARE, 7 - (pos[1] // SQUARE)
        sq = chess.square(col, row)
        if self.sel is None:
            if (p := self.board.piece_at(sq)) and p.color == (self.color == "white"):
                self.sel = sq
        else:
            move = chess.Move(self.sel, sq)
            if move in self.board.legal_moves:
                self.send_move(move.uci())
            self.sel = None

    def draw_chat(self):
        pygame.draw.rect(self.screen, pygame.Color("lightgray"), pygame.Rect(600, 0, 300, HEIGHT))
        y = 10
        for m in self.chat[-10:]:
            self.screen.blit(self.font.render(m, True, pygame.Color("black")), (610, y))
            y += 30
        pygame.draw.rect(self.screen, pygame.Color("white"), pygame.Rect(610, HEIGHT-40, 230, 30))
        self.screen.blit(self.font.render(self.msg, True, pygame.Color("black")), (615, HEIGHT-35))
        pygame.draw.rect(self.screen, pygame.Color("blue"), pygame.Rect(840, HEIGHT-40, 50, 30))
        self.screen.blit(self.font.render("Send", True, pygame.Color("white")), (845, HEIGHT-35))

    def loop(self):
        while self.running:
            self.screen.fill((0, 0, 0))
            self.draw_board()
            self.draw_chat()
            pygame.display.flip()
            for e in pygame.event.get():
                if e.type == pygame.QUIT: self.running = False; pygame.quit()
                elif e.type == pygame.MOUSEBUTTONDOWN:
                    if 840 <= e.pos[0] <= 890 and HEIGHT-40 <= e.pos[1] <= HEIGHT-10:
                        self.send_chat(self.msg)
                    elif self.turn == self.color:
                        self.handle_click(e.pos)
                elif e.type == pygame.KEYDOWN:
                    if e.key == pygame.K_RETURN: self.send_chat(self.msg)
                    elif e.key == pygame.K_BACKSPACE: self.msg = self.msg[:-1]
                    else: self.msg += e.unicode

if _name_ == "_main_":
    ChessClient()
