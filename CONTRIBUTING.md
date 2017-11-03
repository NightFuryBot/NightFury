# Contributing to the Bot.

### Index

+ [Structuring Code](https://github.com/NightFuryBot/NightFury/blob/master/CONTRIBUTING.md#Stucturing-Code)

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

*[More info on structuring code can be found here](https://github.com/NightFuryBot/NightFury/wiki/)*

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
        
    // Init blocks must ALWAYS come BEFORE class level members.
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
        require(param.string = mutableProperty) {
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