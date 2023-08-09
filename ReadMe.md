# Additional NLP components for the VOnDA dialogue management framework

This repository should evolve into a pool of NLP components that can be used with [VOnDA](https://github.com/bkiefer/vonda), e.g., implementations of the
`Interpreter` and `Generator` interfaces.

## rasa NLU interpreter

Currently, only an adapter to a rasa NLU server and some example files for how to create and train such a NLU are here as an example of possibly other adapters.

The Java part connects to an external rasa HTTP server and gets back JSON result structures, which are then converted with the [cplanner](https://github.com/bkiefer/cplan) framework that is already available in VOnDA. The (declarative) converter rules can be changed according to the specific needs of the project, to match the names and the structures chosen in the training data, in the best case without writing additional java code.
