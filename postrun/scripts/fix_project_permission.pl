#!/usr/bin/perl -wT

#Why is this Perl?  Well, because perl has a "sudo-aware" mode, which is VERY paranoid about strings and such getting
#passed around, and forces you to sanitize everything or it breaks.  This is therefore safer to run in sudo mode,
#than another script in some other language might not be; and is WAY safer than running the whole server as root.
use strict;
use warnings;

sub sanitize {
    my $str=shift;

    return undef if(not defined $str);
    $str=~/^(.+)/;
    return $1;
}

our $uid=100;
our $gid=696631985;

if(scalar @ARGV == 0){
    print "This should be installed suid root to ensure project file permissions are set correctly\n";
    print "Usage: mkdir_on_behalf_of /path/to/directory\n";
    exit(1)
}

my $n_updated = chown $uid,$gid,$ARGV[1];
chmod 0664, $ARGV[1];
die "Could not update permissions on ".$ARGV[1] if($n_updated<1);
print "Updated uid/gid on ".$ARGV[1];
