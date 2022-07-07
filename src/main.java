public class main {
    public static void main(String[] args){
        Chip8 chip8 = new Chip8();
        //Load rom into ram

        while(true){
            chip8.decodeExecute(chip8.fetch());
        }
    }
}
