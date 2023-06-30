# Just A Battery

### Support:

- 1.20.1: supported
- 1.20: Bugfix only
- 1.19.4: Bugfix only
- 1.19.3: unsupported
- 1.19.2: Bugfix only
- 1.19: unsupported
- 1.18.x: unsupported
- 1.17.x: unsupported
- 1.16.x: unsupported

### About
Just a Battery is another "Mini-Mod" and adds just a battery to the game.  
This was suggested by **markygnlg**.  

There is only one item added, the battery:  
![battery_single_empty](https://raw.githubusercontent.com/canitzp/JustABattery/master/readme/battery_single_empty.png)  
but it comes in a lot of varieties!  
![tab](https://raw.githubusercontent.com/canitzp/JustABattery/master/readme/tab.png)

## Usage
A battery can have one of 4 modes. These define how the battery behaves:  
0. The Battery does nothing. This mode is the default one. The battery only accepts energy, but it won't push it to other items.
1. First found first gets. Here the battery searches every tick for an item, which can accept its stored energy and tries to fill it.
2. All. The battery gets a list of all energy items within the players inventory and tries to fill all at once (the energy is split between all, that can accept energy).
3. Random. The battery gets one random item of the inventory, which gets the energy.

![](https://raw.githubusercontent.com/canitzp/JustABattery/master/readme/mode.png)

To change modes, just crouch/sneak (default "shift" key) + right-click, with the battery in your hand, on any block.  
It cycles through the modes from 0 to 3 and back to 0.

## Crafting  
Crafting one battery is simple:  
![crafting_bat](https://raw.githubusercontent.com/canitzp/JustABattery/master/readme/crafting_bat.png)  
(Use iron ingots instead of copper ingots for the 1.16.5 version)  

But one battery mostly doesn't help a lot.  
You need more capacity and faster transfer to progress, well there is a simple way of increasing those values.

### Capacity Increase
You simply craft multiple batteries together:  
![crafting_one](https://raw.githubusercontent.com/canitzp/JustABattery/master/readme/crafting_one.png)  
You can really craft them like you want, there is no restriction.
You can for example put batteries in every crafting slot, and you would get a combined battery (If there don't exceed the combined limit).

### Transfer Speed Increase
Like most energy-related items, the battery has a maximum transfer speed, but you can increase it with plain gold nuggets!  
![crafting_one](https://raw.githubusercontent.com/canitzp/JustABattery/master/readme/crafting_two.png)  
Again you decide how much and where to put the batteries and nuggets.

### Mix-it-like-you-want
Well like already said, there are no rules on how to upgrade them.  
You can mix the ingredients as you like, as long as it stays within the predefined bounds (Default: Level & Trace-width of 100).  
![mixitlikeyouwant](https://raw.githubusercontent.com/canitzp/JustABattery/master/readme/mixitlikeyouwant.png)

### Load-By-Lightning
Yes like it says, you can charge up the battery with lightning bolts.
Just be hit as a player, or the lightning strikes the battery on the ground directly, to gain a lot of energy.
I have tested it and it seems to be around 500k FE per lightning bolt.

### Extracting from blocks
It is possible to extract energy from all blocks, that can store energy and also can transfer that energy to others.
These are mostly capacitor blocks or accumulators and things like machines (powered furnace) won't work.

The only thing you have to do is to right-click with a battery in your hand on the block.

Keep in mind that sometimes the clicked side does matter!
For example on an Immersive Engineering Accumulator, you have to click on a side, configured as output!

The amount of transferred energy depends on the "trace width" attribute of the battery as well as the maximum transfer rate of the block.
The lower value is chosen.

This behaviour can be disabled inside the JustABattery configuration ("allow_block_discharge").

### Creeper charging
Be carefully when attacking a creeper.
If you have your battery in hand and it is charged up enough, you could end up powering the creeper.

This behaviour can be disabled inside the JustABattery configuration ("chargeup_creeper_energy_required").