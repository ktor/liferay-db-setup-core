#
# The Nix configuration allows to start up a shell with complete project environment (required tools).
# The environment allows for building and contribution to the repository.
#
# The environment is local- does not mess with the global configuration of your PC.
#
# 1. To start you need to install Nix first: https://nixos.org/guides/install-nix.html
# 2. Next step is to start up the project environment with a command:
#        nix-shell
#
# Project environment consists of:
#  - JDK8 package that is used by Liferay
#  - maven
#  - git
#  - bash shell with command completion
#
with import <nixpkgs> { overlays =
    [( self: super:
    {
        maven = super.maven.override {
            jdk = self.jdk11; # use jdk11 with maven
        };
    }
    )];
};

mkShell {
    buildInputs = [
        jdk11
        maven
        gitAndTools.gitFull
        bash
        bash-completion
    ];
}
