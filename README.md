ThreeBodyOrbit
ThreeBodyOrbit is a JavaFX-based physics simulation project that visualizes the trajectories and dynamics of the three-body problem. The simulation incorporates a softening parameter to prevent collisions and uses the Velocity Verlet method for numerical integration.

Features
Three-Body Problem Simulation: Simulates gravitational interactions and orbital trajectories of three bodies.
Real-Time Animation: Updates positions and velocities dynamically, with trails showing historical motion.
Energy Monitoring: Continuously calculates and logs the system's total energy to ensure physical consistency.
Interactive Visualization: Provides a visually appealing and intuitive display using JavaFX.
Customizable Trails: Each body has a unique trail color for easy tracking.
Technical Details
Programming Language: Java
Graphics Library: JavaFX
Numerical Method: Velocity Verlet integration for motion updates.
Physical Constants:
Gravitational Constant (G): 6.67
Time Step (DT): 0.1
Softening Parameter (EPS): 10.0
How to Run
Running the Simulation
Ensure JDK 8 or higher is installed on your system.
Set up your project to support JavaFX.
Clone or download this repository.
Compile and run the program using the following commands:
bash
复制
编辑
javac ThreeBodyOrbit.java
java ThreeBodyOrbit
Configurable Parameters
You can modify the following parameters in the code to explore different simulation settings:

G: Gravitational constant to adjust the strength of gravity.
DT: Time step for the simulation.
EPS: Softening parameter to avoid collisions.
STEPS_PER_FRAME: Number of simulation steps per frame for smoother motion.
MAX_TRAIL_POINTS: Maximum number of points in the trails to control their length.
Outputs
Trails: Each body leaves a colored trail showing its motion history.
Total Energy: The total energy of the system is logged to the console every 200 frames for monitoring.
Project Structure
ThreeBodyOrbit: The main class, responsible for initialization and animation.
Body: An inner class representing a celestial body and its properties.
computeAcceleration: Calculates gravitational acceleration for each body.
velocityVerletUpdate: Updates the positions and velocities using the Velocity Verlet method.
computeEnergy: Computes the system's total kinetic and potential energy.
Example Screenshot
(Include a screenshot of the running simulation here.)

Educational Relevance
This project is ideal for exploring:

Gravitational dynamics
Numerical integration methods (e.g., Velocity Verlet)
Chaotic behavior in the three-body problem
License
This project is licensed under the MIT License. See LICENSE for details.

Contributions
Contributions are welcome! If you encounter any issues or have suggestions for improvement, please submit them via GitHub Issues.
Not guaranteed to conform to the motion state of the three body problem in the real universe. 
