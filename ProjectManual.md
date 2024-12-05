PROJECT: Real-Time Poker Equity Calculator
Summary: A poker equity calculator computes the probability of winning for different poker hands. Given multiple players' hole cards and community cards (if any), it calculates each player's equity (probability of winning) in real-time. This requires both exact calculations for smaller card sets and Monte Carlo simulation methods for complex scenarios.

● In the first phase, a Java program will be implemented that can represent poker cards, hands, and perform basic equity calculations between two players' hole cards preflop. The program must read hand inputs from a text file in a specified format and output exact equities. The examiners will provide test cases covering various preflop scenarios.

● In the second phase, a playable poker hand analyzer will be implemented where users can input different poker scenarios and receive equity calculations in real-time. The program must support different analysis modes and provide computer-assisted hints for hand strength evaluation.

● In the third phase, the work extends to support multi-player scenarios, ranges instead of exact hands, and post-flop situations. The program must handle much larger calculation spaces efficiently using Monte Carlo simulation. The code will participate in a performance competition against other groups using examiner-provided test scenarios.

1. Project Description

Poker equity calculation is a fundamental concept in poker strategy. For any given poker scenario, we need to determine each player's probability of winning the hand. This probability is called "equity." In its simplest form, this involves comparing two known hands preflop (for example, A♠A♣ vs K♥K♦). However, real poker situations are much more complex, involving:
- Multiple players (2-9)
- Community cards (flop, turn, river)
- Unknown opponent hands (ranges of possible hands)
- Dead cards (cards visible from folded hands)

A basic equity calculation between two known hands can be done through exact enumeration - trying all possible board runouts. For example, when comparing A♠A♣ vs K♥K♦ preflop:
- Total possible boards: 48C5 = 1,712,304
- For each board, determine the winner
- Equity = (number of winning boards) / (total boards)

For more complex scenarios (multiple players, ranges, post-flop), Monte Carlo simulation is required:
- Randomly deal required cards many times
- Track winning percentages
- Use statistical methods to ensure accuracy

General Rules:
The code must be entirely your own - no third-party poker equity calculation libraries may be used. You may not use any libraries that are not included in the Java standard distribution, except for JavaFX. In particular: you may use only JavaFX or Java Swing for graphical components.

All card graphics should use standard poker notation:
- Suits: ♠,♥,♦,♣ or s,h,d,c
- Ranks: 2,3,4,5,6,7,8,9,T,J,Q,K,A

We expect comprehensive testing of your code for correctness. You must demonstrate that your equity calculations match known correct values for test cases. Document your testing methodology and any interesting performance findings in your presentation/report.

Phase 1:
In phase 1, you will write a Java program that reads poker hand scenarios from a text file. Each scenario will specify:
- Two players' hole cards (e.g., "AcAh" vs "KdKs")
- Optional dead cards
- Format: "Hand1:Hand2:DeadCards" (e.g., "AcAh:KdKs:2c3c")

Requirements:
● The program must correctly parse the input format and represent cards internally
● Implement exact equity calculation for preflop scenarios
● Display results showing:
  - Equity percentage for each player
  - Number of possible boards calculated
  - Calculation time
● Handle invalid inputs gracefully (illegal cards, duplicate cards)
● Complete calculations for standard preflop scenarios within 2 seconds
● The examiners will provide test scenarios of increasing complexity

Phase 2:
In phase 2, you will create an interactive poker equity calculator with a graphical interface. Users can input hands in multiple ways and receive real-time equity updates.

The core gameplay/usage has three modes:
- Quick Calc: User inputs exactly two hands and gets immediate exact equity
- Range Analysis: User can input hand ranges and explore equations
- Board Study: User can place specific board cards and see how equities change

Requirements:
● Visual card selection interface showing standard 52-card deck
● Support for three distinct calculation modes
● Real-time equity updates as inputs change
● Invalid hand combinations must be prevented/flagged
● Each mode must include a "hint" system:
  - Quick Calc: Show hand strength category
  - Range Analysis: Suggest stronger/weaker hands in range
  - Board Study: Highlight drawing opportunities
● Results must be displayed both numerically and graphically
● The interface must prevent illegal card selections (same card twice)

Phase 3:
This phase extends the equity calculator to handle complex scenarios through Monte Carlo simulation. The focus is on performance optimization and handling larger calculation spaces.

Requirements:
● Support for:
  - Up to 9 players
  - Hand ranges (e.g., "AK+" or "22-55")
  - Post-flop scenarios
● Monte Carlo simulation with:
  - Configurable number of iterations
  - Progressive accuracy updates
  - Statistical confidence intervals
● Multi-threading support for parallel simulation
● Memory-efficient range representation
● Performance targets:
  - >100,000 hand combinations/second for exact calculation
  - >1,000,000 iterations/second for Monte Carlo
● The system will be tested against hidden test cases involving:
  - Large hand ranges
  - Multi-way pots
  - Complex board textures
