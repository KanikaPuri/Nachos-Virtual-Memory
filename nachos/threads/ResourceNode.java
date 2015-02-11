package nachos.threads;

import nachos.machine.*;
import nachos.threads.*;
import java.util.LinkedList;
import java.util.Iterator;

public abstract class ResourceNode {
    
    public ResourceNode(){
       
    }
    
    /* This is how getEffectivePriority gets the priority with donation from the
     * resource graph.
     */
    public int getMin(){
        return minimumPriority;
    }

    /* Add an edge to the resource graph going from us to node n. Since we added
     * a new path, we need to propagate our value along this path to keep all
     * paths having decreasing priority.
     */
    public void addEdge(ResourceNode n){
        this.outgoing.add(n);
        n.incoming.add(this);

        n.propagate(this.minimumPriority);
    }

    /* Remove the edge from us to node n. Since we took away an incoming edge
     * from n, we need to let it know to fix it's minimum value. Note that node
     * n has no outgoing edges from it so no matter what its new minimum is it
     * wont break the graph
     */
    public void rmEdge(ResourceNode n){
        this.outgoing.remove(n);
        n.incoming.remove(this);

        n.fixMin(this.minimumPriority);
    }
    
    /* Propagate information along the edges of our graph. Here priority is the
     * minimum priority of the incoming node which just changed its priority. If
     * it is lower than our minimum then we found something better. Use it and
     * let our outgoing edges know that we found a better priority.
     */
    private void propagate(int priority){
        if(priority < minimumPriority){
            minimumPriority = priority;

            Iterator iter = outgoing.iterator();
            while(iter.hasNext()){
                ((ResourceNode) iter.next()).propagate(minimumPriority);
            }
        }
    }
    
    /* Fix our minimum once we have lost an incoming edge. priority is the min
     * priority of the edge we just lost. If is equal to our minimum then we
     * know we need to recalculate since it is no longer available to us.
     */
    private void fixMin(int priority){
        if(minimumPriority == priority){
            minimumPriority = myBasePriority;

            Iterator iter = incoming.iterator();
            while(iter.hasNext()){
                int temp = ((ResourceNode) iter.next()).minimumPriority;
                if(temp < minimumPriority)
                    minimumPriority = temp;
            }
        }
    }

    /* Child classes will override this method so that we know how to get the
     * base priority for this node.
     */
    protected abstract int getMyPriority();

    /* Should be called during construction after the sub class has received its
     * priority so that we can use the info in our graph. Should ONLY ever be
     * called during construction.
     */
    protected void setMyPriority(){
        myBasePriority = getMyPriority();
        minimumPriority = myBasePriority;
    }

    /* called as a result of Scheduler.setPriority
     * If we are here, it means that the base priority of this thread has
     * changed due to circumstances outside of our control. We must now fix the
     * the resource allocation graph.
     */
    public void resetPriority(){
        //get new base priority
        myBasePriority = getMyPriority();

        //if it is lower than our minimum, thats fine. Just tell other nodes
        //that we have a new lower priority like usual
        if(myBasePriority < minimumPriority){
            propagate(myBasePriority);
        }
        else if(myBasePriority > minimumPriority){
            // if it is greater, then we have a problem since our minimum
            // priority might actually increase. Normally, this wouldn't be an
            // issue because every other way it could possibly increase we are
            // guarnteed to have no outgoing edges. However, there is no such
            // restriction if we allow a thread's base priority to change at any
            // time.
            resetMin();
        }
    }

    /* Force re-evaluation of minimum in case it went up. If it is higher then
     * force all neighbors to re-evaluate their own minimum.
     */
    private void resetMin(){
        //hold old minimum
        int oldMin = minimumPriority;

        //find minimum of all incoming edges and my new base prioriy
        minimumPriority = myBasePriority;

        Iterator iter = incoming.iterator();
        while(iter.hasNext()){
            int temp = ((ResourceNode) iter.next()).minimumPriority;
            if(temp < minimumPriority)
                minimumPriority = temp;
        }

        //if my minimum went up, the graph could be broken. Force all of my
        //outgoing neighbors to re-evaluate their own minimum in case they were
        //relying on my minimum. This is the step that is usually not required
        //when we evaluate our minimum because we know there are no outgoing
        //edges.
        if(minimumPriority > oldMin){
            iter = outgoing.iterator();
            while(iter.hasNext()){
                ((ResourceNode) iter.next()).resetMin();
            }
        }
    }
    
    private LinkedList incoming = new LinkedList<ResourceNode>();
    private LinkedList outgoing = new LinkedList<ResourceNode>();
    private int myBasePriority;
    private int minimumPriority;
}
