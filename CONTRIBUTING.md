# Contributing to the Bot.

### Index

+ [Pull Request Life Cycle](https://github.com/NightFuryBot/NightFury/blob/master/CONTRIBUTING.md#Pull-Request-Life-Cycle)
+ [Structuring Code](https://github.com/NightFuryBot/NightFury/blob/master/CONTRIBUTING.md#Stucturing-Code)

## Pull Request Life Cycle

NightFury is an open source project that aims to provide many servers with high
quality services such as moderation, utility, music, and more.

Being an open source project, we are open to pull requests, changes to code,
and other public assistance with achieving the goals we have.

If you wish to contribute at all, it's important to understand the staging process
we use.

### 1) Preparation

**BEFORE** you fork the bot it would be best to figure out if your contribution
is going to be welcome or not.

For one, before you do **anything** you should get in contact with the bot's
developers on [NightFury's Support Server](https://discord.gg/XCmwxy8). As we
generally have a good idea whether or not a idea or contribution will be accepted.

NightFury's code is 100% **[Kotlin](https://github.com/Jetbrains/Kotlin/)**, and
contributions (obviously) are also written in Kotlin.

It's recommended that you use [Intellij IDEA](https://www.jetbrains.com/idea/), as
it is created and maintained by Jetbrains (the creators of Kotlin) and has the highest
quality support for the language across all other development environments out there.

>
> #### A Word on Significant Commits
> Pull Requests are made to the bot that make significant changes to code
> or documentation.
>
> The keyword here is __**[significant](https://github.com/NightFuryBot/NightFury/wiki/)**__.
>
> If the PR is not significant, it will be denied without more than a link to our definition
> linked above.
>

### 2) Forking

When you fork the bot, you're going to have to set up a proper git-environment
locally and remotely so that you make proper commits.

**First** clone your new fork (make sure to replace `YourGitHubUsername` with,
well... Your GitHub Username!):

```bash
git clone https://github.com/YourGitHubUsername/NightFury.git
```

**Next** make sure your fork has `upstream` and will stay correctly updated:

```bash
git remote add upstream https://github.com/NightFuryBot/NightFury.git
```

**Finally** create a local branch to make changes to with a fitting name:

```bash
$ git checkout -b command/music-pause upstream/development
```

**Note:** The branch made was based on `development` and not
`master`. More on that later.

>
> #### How To Name Branches Correctly
> Properly named branches are recommended when contributing
> to help us understand the content of the contribution.
> 
> Use the guide below to properly name branches:
>
> If you are submitting a **command** the branch name should
> be formatted as `command/[category]-[name]`.
> + `category` is the `commands` sub-package the command
    file will be located in.
> + `name` is the name of the command, as declared in the code.
>
> If you are submitting documentation the branch name should
> be formatted as `documentation/[description]`.
> + `description` is a keyword or short phrase (shouldn't be
    more than 10 characters or so) that **accurately** describes
    the thing you are documenting.
> 
> If you are committing to an **experimental** or **feature**
> branch of the bot, you should use the formats
> `feature/[branch-name]/[description]` or
> `experimental/[branch-name]/[description]` (respectively).
> + `branch-name` is the name of the experimental or feature branch.
> + `description` is a keyword or short phrase (shouldn't be
    more than 10 characters or so) that **accurately** describes
    the thing you are making changes to.
>
> **Note:** creation of `feature` and `experimental` branches is
> done only by those who have push access to the main repo, or with
> the approval of the aforementioned people (although this is *rare*).
>
> Branch names should replace all spaces with `_` and should opt for
> abbreviations where possible without losing the meaning.
>
> Misleading, or improperly done branch names may lead to a denial in PR
> or even repo blacklisting if malicious.
>
> If your branch touches a lot of areas and cannot be properly named,
> simply naming it `patch-1` will suffice.
>

### 3) Making Changes To Code

Changes to code should be made to the branch that will be used for a future
Pull Request.

Because there is no way to test NightFury, we recommend you stay in touch with
a developer and make commits often to prevent your changes from falling on their
knees when they are reviewed in a PR.

__**COMMENT YOUR CODE**__

Please do not be *that guy*.
We love to see people willing to do work on the bot, but uncomment code makes us
less motivated to review your PR, and it can make or break your chances depending
on what you are working on.

We also have a clear list of changes we will **NEVER** accept under any circumstances
which can be found [here](https://github.com/NightFuryBot/NightFury/wiki/).

### 4) Submitting A Pull Request

Pull requests must be targeted towards the `development` branch, unless explicitly
told otherwise.

> This is why you based your branch off of `upstream/development` and
> not `upstream/master`.<br>
> We do this to prevent malicious changes to code that slip through
> our reviews from ever touching `master` and to make tweaks to PRd code
> before merging it into `master`.<br>
> The process just mentioned is usually quick and does not take more than
> an hour typically.

Make sure to give your PR an accurate name, and a description listing every significant
change you have made to the codebase.

Review can take several days potentially, during which your code might become *stale*
due to higher priority PRs being accepted.<br>
In this case you'll be required to merge the changes to the your remote branch.

```bash
git merge upstream/development
```

**Note:** You may have to *manually* resolve some conflicts if the code you modified was
also modified by the commits you are merging.

If you are not sure how the heck this works, I sincerely recommend you read
[this](https://help.github.com/articles/resolving-a-merge-conflict-using-the-command-line/)
before you touch any buttons.

### 5) You're Done!

There you have it! You've successfully made a contribution to the bot.

We are always looking for contributions and assistance, so don't feel too shy.

If you don't understand a part of the code you are modifying, just visit us on the support
server mentioned throughout this informational and we'll try to assist you as best as we can.

## Structuring Code

All code must be properly structured according to our formatting style.

If you make changes to classes that are improperly formatted, you should also
attempt to clean up any existing code that your changes relate to.

PRs will not be *denied* purely on the basis of one or two formatting
issues, however grossly failing to meet these guidelines will result
in a request to make changes correcting formatting. In the event of 
this, the author of the PR will push **no more than one** reformatted 
commit to correct mistakes.<br>
Failure to do this will result in the either a request to commit changes again,
and/or the PR being rejected.

*More info on structuring code can be found [here](https://github.com/NightFuryBot/NightFury/wiki/)*

__**A Well Formatted Example**__
```kotlin
// Remember!
//
// doingThis: IsGood
// doingThis : IsBad
// doingThis :IsAlsoBad
// doingThis:IsEvenWorse
//
// This applies EVERYWHERE except when extending or implementing
// an abstract class or interface to an object, interface, or class
// that doesn't have a primary constructor, in which case you would
// do
//
// It : LikeThis()

// Always mark the class with your name
// if you made it
/**
 * @author Your Name
 */
class NightFuryClass<T: Closeable>(val constructorClassProperty: Int,
                                   // When possible, put constructor defined properties
                                   // in the primary constructor.
                                   // Also if the constructor gets too long break it up a bit
                                   
                                   // They should also either be aligned like this and end
                                   // with the opening brace OR start on the newline directly
                                   // below the opening parenthesis indented with 4 whitespace.
                                   var anotherOne: Long): Interface {

    // ALWAYS explicitly specify class level property type
    // for non-literals
    val property: Something = Something("foo")
        // The only time you are allowed to lift returns out of a block is
        // when they fit nicely in one line without braces.
        get() = if(mutableProperty.string == "boo") field else Something(mutableProperty)
    
    // You may declare class-level literals like Strings implicitly.
    var mutableProperty = "boo"
        // Annotations generally go above the thing they annotate.
        // If it looks better to place them left of the structure
        // you can do that
        @Suppress("RedundantGetter") get // Declare Getter BEFORE Setter
                                         // You can leave a space between the two if you want
        private set(value) {             // Custom Setters ALWAYS have a block unless it's only specifying visibility
            if(field == value)
                println("You're setting mutableProperty to what it is already! That's redundant!")
            
            field = value
        }
        
    // Init blocks must ALWAYS come AFTER class level members.
    init {
        doSomething()
    }
    
    fun nightFuryFunction(param: Any, otherParam: Any) {
        // Local vals and vars do not have to have typing specified explicitly
        // if declared directly.
        val localVal = Something("local")
        
        val complexAssignment: Something
    }
    
    fun anotherFunction(param: Something): String {
        require(param.string == mutableProperty) {
            "When possible, use require for a if() throw IllegalArgumentException()"
        }
        
        return param.string
    }
    
    // While it's alright to place a companion object at the top
    // of the class, it's generally better and more readable to
    // place it at the bottom of the class where you'd normally put
    // nested classes.
    companion object {
        // Like class level properties and functions, non-literals
        // should have their type specified explicitly.
        val cube: Cube = Cube("Cake")
        
        fun giveCube(): Cube = cube
    }
}

// Package level functions come AFTER the class the file pertains to
// and should primarily be intended for usage in the class (this kind
// of behavior should be used sparingly, and avoided if possible). 
internal fun doSomething() = do { println("Something") } while(false)
```