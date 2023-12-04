package no.uio.gfogtmd;


import java.util.List;

/**
 * A leg consists of the merged segments with the same modeId
 * So a leg is the duration that the user had the same mode of transportation
 */
public class Leg implements Comparable<Leg>{
    private long startTime;
    private long endTime;
    private int modeId;




    public int getModeId(){
        return modeId;
    }
    public void setModeId(int modeId){
        this.modeId = modeId;
    }

    public long getStartTime(){
        return startTime;
    }
    public void setStartTime(long startTime){
        this.startTime = startTime;
    }

    public long getEndTime(){
        return endTime;
    }
    public void setEndTime(long endTime){
        this.endTime = endTime;
    }


    // overriding the compareTo method of Comparable class
    public int compareTo(Leg compareLeg) {
        long compareage
                = ((Leg)compareLeg).getStartTime();

        //  For Ascending order
        return (int) (this.startTime - compareage);

    }



}
