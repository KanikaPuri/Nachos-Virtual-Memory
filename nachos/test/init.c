#define SIZE 30000

int array[SIZE] = {5,5,5,5,5,5,5};

int main(){
    int i;

    for(i = 0; i < SIZE; i++){
        if(array[i] != 0){
            return 1;
        }
    }

    return 0;
}
