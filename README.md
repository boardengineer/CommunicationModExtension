# CommunicationModExtension
Additional Functionality for the STS communication mod that adds socket communication and savestate functions

Installing this mods adds the commands

savestate [statename]

and

loadstae [statename]

save states remain in memory for as long as the the process is active

Socket Communication was added as a simple way to test out script functionality utilzing com mod as a CLI.  In order to enable this:

1) Compile this modules with startSocketCommunication = true
2) start the game and click 'start external process' in the mod menu
3) Launch the java app utilities/SampleClient.java, the command line will start and wait for the first input (you can input an empty line to get a prompt) 
