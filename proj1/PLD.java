import java.util.Random;

public class PLD {
	float pDrop;
    float pDuplicate;
    float pCorrupt;
    float pOrder;
    float pDelay;
    long seed;
    
	public PLD(float pDrop, float pDuplicate, float pCorrupt, float pOrder, float pDelay, long seed) {
		super();
		this.pDrop = pDrop;
		this.pDuplicate = pDuplicate;
		this.pCorrupt = pCorrupt;
		this.pOrder = pOrder;
		this.pDelay = pDelay;
		this.seed = seed;
	}

	public int roller() {
		
		System.out.println("Seed: " + this.seed);
		Random random = new Random(this.seed);
		int choice = 0;
		if (random.nextFloat() < this.pDrop) {
			System.out.println("probality1: ");
			choice = 1;
		}
		else if (random.nextFloat() < this.pDuplicate) {
			System.out.println("probality2: ");
			choice = 2;
		}
		else if (random.nextFloat() < this.pCorrupt) {
			choice = 3;
		}
		else if (random.nextFloat() < this.pOrder) {
			choice = 4;
		}
		else if (random.nextFloat() < this.pDelay) {
			choice = 5;
		}
		return choice;
	}
    
}
