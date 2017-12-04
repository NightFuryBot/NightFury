This cleans the channel it is called in of up to 1000 messages that are less 
than two weeks old.

Several flags can be specified to delete messages by, and are listed below:

`userID` or `@user` - Only deletes messages by a user.<br>
`bots` - Only deletes messages by bots.<br>
`embeds` - Only deletes messages containing embeds.<br>
`links` - Only deletes messages containing links.<br>
`files` - Only deletes messages containing file uploads.<br>
`images` - Only deletes messages containing images uploads.<br>

It's worth noting the order above is the exact order of which this command 
discerns what to delete by. This in term, prevents flags from being combined 
to specify past their original intent.
An example is that by using the `bots` flag, you will inevitably delete all 
`files` uploaded by that bot. However, using the flags in combination (`bots files`) 
will not produce an effect where only messages from bots containing files are 
deleted.

As a final note, discord prevents the bulk deletion of messages older than 2 weeks by bots. As a result, NightFury, nor any other bot, is able to bulk clean a channel of messages that were sent two weeks prior to the command being used.