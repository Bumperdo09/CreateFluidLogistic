# Welcome to Create Fluid Logistic

This mod expands the logistics system of Create 6.0, adding a fluid logistics system suitable for various fluids.

The mod only supports Create version 6.0 and above!

## Item Function Overview

### Waterproof Cardboard Block

Used to craft the Fluid Packager.

### Compressed Storage Tank

A single Compressed Storage Tank has a capacity of 10B, and a single Fluid Package can contain up to nine. It can interact with fluid tanks; upon interaction, the Compressed Storage Tank is destroyed, and the fluid inside is transferred to the fluid tank. When the fluid in a Compressed Storage Tank is ≤1B, unpacking it into a Basin or throwing the tank into a Basin will store the fluid in that Basin. Compressed Storage Tanks in item containers are not displayed as fluids in the logistics network but are shown as items.

### Fluid Packager

The Fluid Packager shares many similarities with the regular Packager. The difference is that when connected to a fluid container, it virtualizes the fluids from that container and displays them in the Stockkeeper's GUI. It can only package fluids into Fluid Packages or unpack Fluid Packages back into fluids, storing the fluids in the connected fluid container.


### Fluid Package

A Fluid Package is a rare, waterproof package that cannot be obtained by cycling in a regular Packager. A single Fluid Package has a maximum capacity of 90B and can only contain Compressed Storage Tanks. Its appearance is rendered based on the capacity and type of the Compressed Storage Tanks inside. Both the regular Packager and the Fluid Packager can unpack this type of package. However, when a regular Packager unpacks it, the Compressed Storage Tanks are stored in the item inventory connected to the Packager, whereas the Fluid Packager unpacks it to store the corresponding fluid in the fluid container connected to the Fluid Packager.

