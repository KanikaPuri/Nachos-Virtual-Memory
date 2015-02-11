package nachos.threads;

public class AdderThread implements java.lang.Runnable {
    static int count = 0;
    private int amount;
    private String id;

    public AdderThread(String id, int amount){
        this.id = id;
        this.amount = amount;
    }

    public void run(){
        for(int i = 0; i < 5; i++){
            count += amount;
            System.out.println(id + ": count = " + count);
        }
    }
}
