import co.touchlab.skie.configuration.EnumInterop
import co.touchlab.skie.configuration.FunctionInterop
import co.touchlab.skie.configuration.SealedInterop
import gradle.kotlin.dsl.accessors._9dc14a17e826bbd72273f1ed7425b8a0.skie

plugins {
    id("co.touchlab.skie")
}

skie {
    features {
        coroutinesInterop.set(false)
        group {
            EnumInterop.Enabled(false)
            SealedInterop.Enabled(false)
            FunctionInterop.FileScopeConversion.Enabled(false)
        }
    }
}
