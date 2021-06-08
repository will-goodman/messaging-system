Solution Repository:

For this system I decided to separate the threads from the users, so that you could login as one user, logout, and then login as another.

This means that when a client thread is started, it is assigned a name by the server. If a user logs in on that thread, then the name of the thread is changed by the server to that of the logged in user. When that user logs out, it is changed back. This means that the quit method will always work whether someone is logged in or not, but the currently logged in user can still receive their messages.

For the quit system, it works by sending a message to itself, which acts like a boomerang and bounces back to close all the threads. I needed to ensure that another user couldn't send "quit" as a message and log out another user by closing the ServerSender and ClientReceiver threads. To do this I checked in ServerSender if the user who sent the message is the recipient themselves, if yes then the threads are ended, if not then it is sent as a regular message.

For register, I decided to leave the user logged in after registering. I did this because it seemed unnecessary to ask for a username, and then require it again immediately after. When you make a Facebook account it doesn't log you out as soon as you've finished making it, so I applied the same here.

I haven't allowed the same user to log in on multiple threads. This is because it will be complicated to change my current code to enable this, and I believe I will probably break something.

Logout does not end the threads. As said earlier, you can logout and then login again. I did this because I felt that it was pointless when quit would achieve the same, and there should be a difference between the two commands.

When a message is sent, it is added to the recipient's MessageList (if the recipient exists of course), and if they are also logged in then it is sent to the user through their thread's BlockingQueue. If they are not logged in, then when they do log in they'll be able to access it.

When a user logs in, their most recent message is displayed (if any).
When a message is deleted, the next message is shown (if any), if one doesn't exist then the previous message is shown, and if that doesn't exist then a "No more messages" message is displayed. I did this because when a message is deleted, the next message will fall down into its position in the ArrayList, so it seemed to make sense to just display that message.

I added no ability to delete a user, as no such feature was stated in the requirements.

//EXTRA FEATURE:
This version can be found in the extra_feature folder within the repository.

I chose to do message encryption and passwords (with hashing).

For the message encryption, I encrypted messages between the threads, not for users. That is to say, that each Client created a public and private key pair, not each user.

The Server also creates a key pair when it starts.

The public keys are stored in a text file for everyone to access. This is not ideal, as it only really works as it is when connecting to localhost. I struggled to find a way to get the public keys where they needed to be, and this is the only solution I found.

I chose to do RSA encryption, with bit size 2048. I chose this because this is what the US Federal Government decided to use, and if it's strong enough for them, I believe it is string enough for me.

For password hashing I used SHA1. It is not especially secure, but I knew it would work in Java, so I would not have to worry about compatability issues. For the salt, I used 32 bit salt, as I believed it offered a good balance between security and memory use.

A new salt is generated for each user.

When a password is entered, it is encrypted to be transfered from the user to the server. It is then decrypted, and then hashed.
