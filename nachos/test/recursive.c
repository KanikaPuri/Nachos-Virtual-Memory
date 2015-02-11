//calls a recursive function to exercise the stack during program execution
//should return the number 100

int count(int n){
    if(n == 0)
        return 0;

    return 1 + count(n - 1);
}

int main(){
    return count(100);
}
