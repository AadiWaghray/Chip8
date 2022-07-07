import javax.swing.*;
import java.awt.*;
import java.util.Stack;
import java.util.Timer;

public class Chip8 {
    //TODO: I REALLY need to do unit tests.
    //TODO: I need to be really careful about sideffects and find cleaner implementations once the code I have works.
    //TODO: Old chip8 programs would start from 0x200 since the first memory spots were used for the interpreter itself.
    //TODO: Store font somewhere.
    //TODO: Get content panel? https://cs.lmu.edu/~ray/notes/javagraphics/
    //TODO: Keyboard
    //TODO: Screen
    //TODO: How do I time operations in java?
    //TODO: The bitwise shift operation doesn't work as expected so alter implementation.(fetch)
    //TODO: What should the size of the frame be?
    //TODO: Implement BNNN or BXNN configurable
    //TODO: Make vx = vy configureable.(0x6)
    //TODO: Check if java overflow rules will break the emulation.(0x7)
    Chip8(){
        Frame window = new Frame();

        int frameWidth = 2560;
        int frameHeight = 1600;
        window.setSize(frameWidth,frameHeight);

        window.add(screen);

        window.setVisible(true);
    }

    //Screen
    public static class Screen extends JPanel {
        public boolean[][] pixelGuide = new boolean[32][64];

        public void paintComponent(Graphics g){
            super.paintComponent(g);

            for(int a = 0; a < getHeight()/32; a++){
                for(int b = 0; b < getWidth()/64; b++){

                    if(pixelGuide[a][b]){g.setColor(Color.WHITE);}
                    else{g.setColor(Color.BLACK);}

                    g.fillRect(a * 32,b * 64, getWidth()/32, getHeight()/64);
                }
            }
        }
    }

    Screen screen = new Screen();

    //Storage
    byte[] ram = new byte[4096];
    byte[] registers = new byte[16];
    short indexRegister;

    short programCounter = 0;

    //Stack
    Stack<Short> stack = new Stack<Short>();

    //Timers
    Timer delay = new Timer();
    Timer sound = new Timer();

    //Fetch instruction from ram and increment PC.
    short fetch(){
       short instruction = (short) (this.ram[this.programCounter] << 8 & this.ram[this.programCounter + 1]);
       this.programCounter =+2;
       return instruction;
    }

    //Figure out instruction/ releavent data from instruction and execute.
    void decodeExecute(short instruction){
        byte N = (byte) (instruction & 0b0000000000001111);
        byte NN = (byte) (instruction & 0b0000000011111111);
        short NNN = (short) (instruction & 0b0000111111111111);
        byte vx = 0b0000111100000000 >> 8;
        byte vy = 0b0000000011110000 >> 4;
        byte x = this.registers[0b0000111100000000 >> 8];
        byte y = this.registers[0b0000000011110000 >> 4];

        switch (instruction & 0b1111000000000000 >> 12){
            case 0x0:
                //How does decoder differentiate from NNN when NNN = 0EE and the opcode 00EE?
                switch (instruction & 0b0000000000001111){
                    case 0x0:
                        break;
                    case 0xE:
                        break;
                    default:
                    //Is default good enough to catch NNN case?
                }
                break;
            case 0x1:
                this.programCounter = NNN;
                break;
            case 0x2:
                this.stack.push(this.programCounter);
                this.programCounter = NNN;
                break;
            case 0x3:
                //Why are skip calls usually followed by jump calls?
                if(x == NN){
                    this.programCounter =+ 2;
                }
                break;
            case 0x4:
                if(x != NN){
                    this.programCounter =+ 2;
                }
                break;
            case 0x5:
                if(x == y){
                    this.programCounter =+ 2;
                }
                break;
            case 0x6:
                this.registers[vx] = NN;
                break;
            case 0x7:
                this.registers[vx] = (byte) +NN;
                break;
            case 0x8:
                switch (instruction & 0b0000000000001111){
                    case 0x0:
                        this.registers[vx] = y;
                        break;
                    case 0x1:
                        this.registers[vx] = (byte) (x|y);
                        break;
                    case 0x2:
                        this.registers[vx] = (byte) (x&y);
                        break;
                    case 0x3:
                        this.registers[vx] = (byte) (x^y);
                        break;
                    case 0x4:
                        //Why no branch?
                        if (x+y>255){
                            this.registers[15] = 1;
                            this.registers[vx] = (byte) (x+y);
                        }else{
                            this.registers[vx] = (byte) (x+y);
                        }
                        break;
                    case 0x5:
                        this.registers[vx] = (byte) (x-y);
                        break;
                    case 0x6:

                        //Is there a difference between assigning a literal or a variable?
                        this.registers[vx] = y;
                        this.registers[vx] = (byte) (this.registers[vx] >> 1);
                        this.registers[15] = (byte) (this.registers[vx] & 0b00000001);
                        break;
                    case 0x7:
                        this.registers[vx] = (byte) (y-x);
                        break;
                    case 0xE:
                        this.registers[vx] = y;
                        this.registers[vx] = (byte) (this.registers[vx] << 1);
                        this.registers[15] = (byte) ((this.registers[vx] & 0b10000000)>> 7);
                        break;
                }
                break;
            case 0x9:
                //Why is this always false?
                if(x != y){
                    this.programCounter =+ 2;
                }
                break;
            case 0xA:
                indexRegister = NNN;
                break;
            case 0xB:
                //Skipped and probably not important.
                //TODO: Check how I need to add these two numbers.
                break;
            case 0xC:
                this.registers[vx] = (byte) Math.floor(Math.random() * 255);
                break;
            case 0xD:
                //Why is x%64 == x&63??
                byte xCoordinate = (byte) (x % 64);
                byte yCoordinate = (byte) (x % 32);
                this.registers[15] = 0;
                for (int a = 0; a < 8; a++){
                    byte row = this.ram[this.indexRegister + a];
                    for( int b = 0; b < 8; b++){
                        if(xCoordinate > 64){break;}
                        if(screen.pixelGuide[xCoordinate][yCoordinate] & (row & 2^b) == 1){
                            screen.pixelGuide[xCoordinate][yCoordinate] = false;
                            this.registers[15] = 1;
                        }else if(!screen.pixelGuide[xCoordinate][yCoordinate] & (row & 2^b) == 1){
                            screen.pixelGuide[xCoordinate][yCoordinate] = true;
                        }
                        xCoordinate ++;
                    }
                    yCoordinate++;
                }
                screen.repaint();
                break;
            case 0xE:
                //Skipped
                switch (instruction & 0b0000000000001111){
                }
                break;
            case 0xF:
                /*
                0xFX1E
                this.indexRegister =+ x;
                 */
                //Skipped
                break;
        }
    }
}

