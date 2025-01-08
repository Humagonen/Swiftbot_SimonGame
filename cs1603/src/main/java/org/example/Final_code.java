package org.example;
import java.util.ArrayList;
import java.util.Scanner;
import swiftbot.*;

/* Notes on how the variables are assigned:

0. Front Left Light  : Red    ->  Button A
1. Front Right Light : Green  ->  Button B
2. Middle Left Light : Blue   ->  Button X
3. Middle Right Light: White  ->  Button Y

There are no lives, the task does not require that.
 */


public class Final_code {

	private static int score = 0;  // define score with an initial value of 0
	private static int round = 0; // counting rounds of the game

	private static ArrayList<Integer> sequence = new ArrayList<>(); // lights sequence as arraylist
	static SwiftBotAPI swiftBot;
	private static boolean inputValid = false;
	private static ArrayList<Integer> userSequence = new ArrayList<>(); // user clicks which button? stores as an arraylist

	public static void main(String[] args) throws InterruptedException {
		try {
			swiftBot = new SwiftBotAPI();
		} catch (Exception e) {
			System.out.println("Failed to initialize SwiftBotAPI.");
			/*
			 * Outputs a warning if I2C is disabled. This only needs to be turned on once,
			 * so you won't need to worry about this problem again!
			 */
			System.out.println("\nI2C disabled!");
			System.out.println("Run the following command:");
			System.out.println("sudo raspi-config nonint do_i2c 0\n");
			System.out.println(e);
			System.exit(5);
		}

		//ASCII art
		greeting();

		Scanner reader = new Scanner(System.in);

		// start the game? 
		while(true) {  
			System.out.println("Are you ready to start?\n");
			System.out.println(
					"1 = Yes         \t|\t 2 = No\n"
					);
			System.out.print("Enter a number: ");
			// read the users input
			String ans = reader.next();

			switch(ans) {
			case "1":
				startGame();   // start the game if yes
				break;
			case "2":
				reader.close();
				System.exit(0);
				break;
			default:
				System.out.println("ERROR: Please enter a valid number"); // will give an error if anything else is entered
			}
		}

	}


	// Game logic
	public static void startGame() {
		while (true) {
			round++;
			System.out.println("Round: " + round + " | Current Score: " + score);

			// each round it will add a random color to the sequence 
			addRandomColor();  
			displaySequence(); // display the colors in each round

			// Wait for user input via buttons
			inputValid = false;
			userSequence.clear(); // each round user has to start over, so remove all from the array
			enableButtons();

			// Wait until user finishes input
			while (!inputValid) {
				try {
					Thread.sleep(100); // Small delay to avoid busy-waiting
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			// Validate user input
			if (!validateUserInput()) {  // if the 2 arrays are not equal then game over
				System.out.println("GAME OVER!");
				System.out.println("Final Score: " + score);
				celebrate(score);
				round = 0;
				score = 0;
				sequence.clear();
				return;
			}

			// else just increase the score
			score++;
			System.out.println("Correct! Score: " + score);

			// Prompt to quit after every 5 rounds
			if (round % 5 == 0) {
				System.out.println("You've completed " + round + " rounds.");
				System.out.println("Would you like to continue or quit?");
				System.out.println("1 = Continue         \t|\t 2 = Quit");

				Scanner reader = new Scanner(System.in);
				String choice = reader.nextLine();

				if (choice.equals("2")) {
					System.out.println("SEE YOU NEXT TIME!");
					System.out.println("Final Score: " + score);
					celebrate(score);
					return;
				}
			}
		}
	}


	// Add a random color to the sequence
	private static void addRandomColor() {
		int randomColor = (int) (Math.random() * 4); // 0-3 for 4 colors (random generates values between [0-1) )
		sequence.add(randomColor);  // adding new random color to the arraylist
	}


	// Display the sequence using LEDs
	private static void displaySequence() {
		System.out.println("Memorize the sequence:");

		for (int color : sequence) {
			flashLED(color);
			try {
				Thread.sleep(500); // Pause between flashes
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}


	// Flash a specific LED
	private static void flashLED(int colorIndex) {
		int[] rgb;
		Underlight underlight;

		switch (colorIndex) {
		case 0: // Red
			rgb = new int[]{255, 0, 0};
			underlight = Underlight.FRONT_LEFT;
			break;
		case 1: // Green
			rgb = new int[]{0, 255, 0};
			underlight = Underlight.FRONT_RIGHT;
			break;
		case 2: // Blue
			rgb = new int[]{0, 0, 255};
			underlight = Underlight.MIDDLE_LEFT;
			break;
		case 3: // White
			rgb = new int[]{255, 255, 255};
			underlight = Underlight.MIDDLE_RIGHT;
			break;
		default:
			throw new IllegalArgumentException("Invalid color index: " + colorIndex);
		}

		try {
			swiftBot.setUnderlight(underlight, rgb);
			Thread.sleep(400); // Blink duration
			swiftBot.disableUnderlight(underlight);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	// Enable button input for the game
	private static void enableButtons() {
		try {
			swiftBot.enableButton(Button.A, () -> handleButtonPress(0)); // Red
			swiftBot.enableButton(Button.B, () -> handleButtonPress(1)); // Green
			swiftBot.enableButton(Button.X, () -> handleButtonPress(2)); // Blue
			swiftBot.enableButton(Button.Y, () -> handleButtonPress(3)); // White
		} catch (Exception e) {
			System.out.println("Failed to enable buttons: " + e.getMessage());
		}
	}

	// Handle button presses
	private static void handleButtonPress(int colorIndex) {
		userSequence.add(colorIndex);  // adds the new color button to the array
		if (userSequence.size() == sequence.size()) {
			try {
				Thread.sleep(500);
				inputValid = true; // Signal that input is complete
				disableButtons();

			} catch (Exception e) {
				// TODO: handle exception
				System.out.println(e);
			}
		}
	}

	// Disable all buttons
	private static void disableButtons() {
		try {
			swiftBot.disableAllButtons();
		} catch (Exception e) {
			System.out.println("Failed to disable buttons: " + e.getMessage());
		}
	}

	// Validate user input
	private static boolean validateUserInput() {
		return userSequence.equals(sequence);  // Array1.equals(Array2) 
		// will check if both arrays are the same, returns true or false
	}





	// Celebration method
	public static void celebrate(int score) {
		System.out.println(score);
		// Cap the score at 30 to make sure bot doesnt go too fast.
		if (score > 30) {
			System.out.println("Score exceeds the maximum allowed for celebration. Using 30 as the score.");
			score = 30;
		}
		int speed = (score < 5) ? 40 : Math.min(score * 10, 100); // Below 5 score, the speed is set to 40 at 5 and above 5 score, the speed changes based on score.
		System.out.println("Celebration Speed: " + speed);	
		int time = 1500;
		try {
			// Blink LEDs in random order before starting
			blinkLEDsRandomly();

			// unit for speed?
			// unit for distance 30 cm
			// unit for time milliseconds
			//			
			//			distance = time * speed
			//			time = distance/speed
			//			time = 30 cm/speed
			switch (speed) {
			case 50:
				time = 1380; // Example time in milliseconds for speed 50
				break;
			case 60:
				time = 1264;  // Example time in milliseconds for speed 60
				break;
			case 70:
				time = 1148;  // Example time in milliseconds for speed 70
				break;
			case 80:
				time = 1032;  // Example time in milliseconds for speed 80
				break;
			case 90:
				time = 916;  // Example time in milliseconds for speed 90
				break;
			case 100:
				time = 800;  // Example time in milliseconds for speed 100
				break;
			default:
				System.out.println("Invalid speed");
			}


			// Move in a V-shape (30 cm per arm HAVENT ADJUSTED YET - need testing)
			// Forward left arm of the V
			swiftBot.move(0, speed, 500);
			Thread.sleep(500); // Adjust timing for 30 cm

			swiftBot.move(speed, speed, time); 
			Thread.sleep(500); // Adjust timing for 30 cm

			// Back to the starting point
			swiftBot.move(-speed, -speed, time);
			Thread.sleep(500);

			// Forward right arm of the V
			swiftBot.move(speed , 0, 1000);
			Thread.sleep(500); // Adjust timing for 30 cm

			swiftBot.move(speed, speed, time);
			Thread.sleep(500); // Adjust timing for 30 cm

			// Back to the starting point
			swiftBot.move(-speed, -speed, time);
			Thread.sleep(500);

			swiftBot.move(0, speed, 500);
			Thread.sleep(500); // Adjust timing for 30 cm
			
			blinkLEDsRandomly();

		} catch (InterruptedException e) {
			System.out.println("Error during celebration: " + e.getMessage());
		}
	}

	private static void blinkLEDsRandomly() throws InterruptedException {
		int[] colors = {0, 1, 2, 3};
		shuffleArray(colors);

		for (int color : colors) {
			flashLED(color); 
			Thread.sleep(300); 
		}
	}

	// Shuffle an array
	private static void shuffleArray(int[] array) {
		for (int i = array.length - 1; i > 0; i--) {
			int j = (int) (Math.random() * (i + 1));
			int temp = array[i];
			array[i] = array[j];
			array[j] = temp;
		}
	}


	// Greeting message
	public static void greeting() {
		System.out.println("");
		System.out.println("  __  __  ______  __  __   ____    ____   __     __");
		System.out.println(" |  \\/  ||  ____||  \\/  | / __ \\  ||   \\  \\ \\   / /");
		System.out.println(" | \\  / || |__   | \\  / || |  | | ||   ||  \\ \\_/ / ");
		System.out.println(" | |\\/| ||  __|  | |\\/| || |  | | ||___//   \\   /  ");
		System.out.println(" | |  | || |____ | |  | || |__| | ||   \\     | |   ");
		System.out.println(" |_|  |_||______||_|  |_| \\____/  ||    \\    |_|   ");
		System.out.println();
		System.out.println("   _____          __  __ ______ ");
		System.out.println("  / ____|   /\\   |  \\/  |  ____|");
		System.out.println(" | |  __   /  \\  | \\  / | |__   ");
		System.out.println(" | | |_ | / /\\ \\ | |\\/| | __|  ");
		System.out.println(" | |__| |/ ____ \\| |  | || |____ ");
		System.out.println("  \\_____/_/    \\_\\_|  |_||______|");

		System.out.println("");
		System.out.println("\t\t\tThe rules are as follows");
		System.out.println("1. Memorize the sequence of colors displayed.");
		System.out.println("2. Press the buttons in the correct order.");
		System.out.println("3. Each correct answer increases your score.");
		System.out.println("4. The game ends if you press the wrong sequence.");
		System.out.println("5. After every 5 rounds, you can quit or continue the game.");
		System.out.println("");
		System.out.println("Press A: Red");
		System.out.println("Press B: Green");
		System.out.println("Press X: Blue");
		System.out.println("Press Y: White");
		System.out.println("\n");
	}

}

