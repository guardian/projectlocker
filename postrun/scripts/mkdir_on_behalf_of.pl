#!/usr/bin/perl -wT

#Why is this Perl?  Well, because perl has a "sudo-aware" mode, which is VERY paranoid about strings and such getting
#passed around, and forces you to sanitize everything or it breaks.  This is therefore safer to run in sudo mode,
#than another script in some other language might not be; and is WAY safer than running the whole server as root.
use warnings;
use strict;
use File::Path qw/make_path/;
use Data::Dumper;
use File::Find;

sub sanitize {
    my $str=shift;

    return undef if(not defined $str);
    $str=~/^(.+)/;
    return $1;
}

our $uid;
our $gid;

#http://stackoverflow.com/questions/3738295/how-do-i-recursively-set-read-only-permission-using-perl
sub update_file_perm {
    my $perm = -d $File::Find::name ? 0775 : 0664; #include execute permissions if it's a directory

    my $n_modified = chmod $perm, sanitize($File::Find::name);
    die "Unable to set file mode on ".$File::Find::name if($n_modified<1);
    $n_modified = chown $uid, $gid, sanitize($File::Find::name);
    die "Unable to set file ownership on ".$File::Find::name if($n_modified<1);
}

$ENV{'PATH'} = "/bin:/usr/bin:/usr/local/bin";

if(scalar @ARGV == 0){
	print "This should be installed suid root to create a directory on behalf of another user.\n";
	print "Usage: mkdir_on_behalf_of /path/to/directory [username] [groupname] [--fixmode]\n";
	exit(1)
}

my $dirname=sanitize($ARGV[0]);
my $username=sanitize($ARGV[1]);
my $groupname=sanitize($ARGV[2]);

my $fixmode=0;
$fixmode=1 if($ARGV[3] eq '--fixmode');

if(! -d "$dirname"){
    print "Making '$dirname' with owner $username and group $groupname";
    my $n_created = make_path($dirname);
    die "\nUnable to create directory $dirname" if($n_created<1);
}

if($username=~/^\d+$/){
	$uid=$username;
} else {
	$uid=getpwnam($username);
}

if($groupname=~/^\d+$/){
	$gid=$groupname;
} else {
	$gid=getgrnam($groupname);
}

find({untaint=>1, untaint_pattern=>qr|^([-+@\w\s./]+)$|, untaint_skip=>1, wanted=>\&update_file_perm}, $dirname);
#my $n_modified = chmod 0664,$dirname;
#die "Unable to set file mode on $dirname" if($n_modified<1);
#
#$n_modified = chown $uid,$gid,$dirname;
#die "Unable to set file ownership on $dirname" if($n_modified<1);

