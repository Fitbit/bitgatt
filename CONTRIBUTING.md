# How to contribute

Thank you so much for considering helping us! We are stoked about all contributions, but especially those in the way of bug reports and strategies for handling phones that have odd behavior.

If you want to see an example of a strategy, check out HandleTrackerVanishingUnderGattOperationStrategy.java.  Basically you can subclass Strategy.java and you are off to the races.  If you are
looking for an example of where to put a hook, after adding your strategy to StrategyProvider.java, you can take a look at
ReadGattDescriptorTransaction.java to see how to call it.  Strategies are handy ways to encapsulate creative solutions to issues around the GATT.

## Testing

All PRs should have tests associated with them, preferrably unit tests, though InstrumentedTests are a bonus.

## Coding Conventions

When in doubt please follow Google's Android coding conventions, we have added a checkstyle.xml for your convenience that illustrates our preferred coding style.

Thanks,
  Irvin Owens Jr, Fitbit Android Engineering
