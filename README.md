# PocketSafe

Have you or someone you know ever got their phone stolen in a bus or train?
PocketSafe is a simple solution to this growing probelem, especially in crowded spaces, which collectively costs huge losses to the masses. The app is designed to alert you whenever your phone is taken out of your pocket without your consent.

### How?
The trick is to use Proximity sensor of your phone to detect the action of pick-pocketing, and then giving the person a grace time of [5 secs] to enter their password [to prove that they are the owner of this device], if the person who took the phone out is not able to put in the password in the given time frame, the phone will sound an alarm at the highest volume which won't stop until the password is provided. Hence alerting the owner.

### Usage
A single press of this button is all that is required to protect your device from theft.

![image](https://user-images.githubusercontent.com/29260302/37256883-c56d93d0-2586-11e8-9c1f-cef91c9980ad.png)

### Details
This simple UI has this trigger button, which-
1. Starts a timer which waits for you to put your phone in your bag/pocket
2. When the phone is inside, the proximity sensor is started
3. Now as soon as the phone is taken out of the pocket, this action is sensed by the phone
4. And an alarm in MAXIMUM VOLUME is started, which can only be stopped when the owner unlocks the device through password.

### Settings
![image](https://user-images.githubusercontent.com/29260302/37256955-29e9434e-2588-11e8-8cbc-65f09a344d11.png)

Several preferences are available which can be modified according to the will of the owner:

1. **Countdown timer sound**: Play a countdown sound [a 'ting' sound played every second] between the action of the phone being taken out and the action of password being entered.
2. **Use flashlight with alarm**: This option can be enabled to rapidly flash the flashlight of your phone (in case of the person not being able to authorize himself by entering password) to give a visual cue in dark places along with the default audio alarm.
3. **Add a separate password to alarm**: For the users who like to keep their phones without any password protection, this option enables them to require a separate password [specified by them, ofcourse] to stop the alarm once sounded.
4. **Grace time to unlock phone**: Sets the number of seconds the user will require (approximately) to enter the passoword. This is the time between the action of the phone being taken out and the action of password being entered.
