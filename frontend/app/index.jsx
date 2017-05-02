import React from 'react';
import {render} from 'react-dom';
import StorageListComponent from './StorageComponent.jsx';
import DefaultComponent from './DefaultComponent.jsx';
import RootComponent from './RootComponent.jsx';
import ProjectTemplateIndex from './ProjectTemplateIndex.jsx';
import FileEntryList from './FileEntryList.jsx';
import ProjectEntryList from './ProjectEntryList.jsx';
import ProjectTypeList from './ProjectTypeList.jsx';

import StorageMultistep from './multistep/StorageMultistep.jsx';

window.React = require('react');
const M_UNKNOWN=-1;
const M_ROOT=0;
const M_STORAGES=1;
const M_TYPES=2;
const M_TEMPLATES=3;
const M_PROJECTS=4;
const M_FILES=5;

const A_UNKNOWN=-1;
const A_LIST=1;
const A_EDIT=2;
const A_DELETE=3;

class ComponentRouter extends React.Component {
    constructor(props){
        super(props);
        this.state = {
            'mode': M_ROOT,
            'action': A_LIST,
            'params': [],
            'target': null
        };

        this.verbAssociation = {
            'list': A_LIST,
            'edit': A_EDIT,
            'new': A_EDIT,
            'delete': A_DELETE
        };

        this.methodAssociation = {
            'storages': M_STORAGES,
            'types': M_TYPES,
            'templates': M_TEMPLATES,
            'projects': M_PROJECTS,
            'files': M_FILES
        };

        this.routings = [
            ['^/$',M_ROOT,A_UNKNOWN],
            ['^/(\\w+)/(\\w+)$',-1,-1],
            ['^/(\\w+)/(\\w+)/(\\d+)$',-1,-1],
        ];

        this.compiledRoutings = this.routings.map((entry)=>{
            return [new RegExp(entry[0]),entry[1],entry[2]]
        });
    }

    componentWillMount() {
        this.setState({target: this.match() });
    }

    match() {
        console.log("Current url path: " + location.pathname);

        const target = this.compiledRoutings.map((entry)=>{
            const result = entry[0].exec(location.pathname);
            if(result==null) return null;

            if(result[3]) return {
                'verb': this.verbAssociation[result[1]],
                'method': this.methodAssociation[result[2]],
                'objectid': parseInt(result[3])
            };
            if(result[2]) return {
                'verb': this.verbAssociation[result[1]],
                'method': this.methodAssociation[result[2]],
                'objectid': null
            };
            if(!result[1]) return {
                'verb': entry[1],
                'method': entry[2]
            }
        }).filter((entry)=>entry!=null);

        if(target.length==0) return [M_UNKNOWN, A_UNKNOWN];
        console.debug(target);
        return target[0];
    }

    render() {
        console.log(this.state.target);
        console.log("Method is " + this.state.target.method);
        console.log("Verb is " + this.state.target.verb);

        switch(this.state.target.method){
            case M_STORAGES:
                if(this.state.target.verb==A_EDIT){
                    return(<StorageMultistep mode={this.state.target.verb} currentEntry={this.state.target.objectid}/>)
                } else {
                    return (<StorageListComponent title="Storages" mode={this.state.target.verb}/>);
                }
            case M_TEMPLATES:
                return(<ProjectTemplateIndex title="Templates" mode={this.state.target[1]}/>);
            case M_ROOT:
                return(<RootComponent title="None" mode={this.state.target[1]}/>);
            case M_FILES:
                return(<FileEntryList title="Files" mode={this.state.target[1]}/>);
            case M_PROJECTS:
                return(<ProjectEntryList title="Projects" mode={this.state.target[1]}/>);
            case M_TYPES:
                return(<ProjectTypeList title="Project Types" mode={this.state.target[1]}/>);
            default:
                return(<DefaultComponent title="None" mode={this.state.target[1]}/>);
        }
    }
}

class App extends React.Component {
    render () {
        return(
            <div>
                <div id="leftmenu" className="leftmenu">
                    <ul className="leftmenu">
                        <li><a href="/list/storages">Storages...</a></li>
                        <li><a href="/list/types">Project Types...</a></li>
                        <li><a href="/list/templates">Project Templates...</a></li>
                        <li><a href="/list/projects">Projects...</a></li>
                        <li><a href="/list/files">Files...</a></li>
                    </ul>
                </div>
                <div id="mainbody" className="mainbody">
                    <ComponentRouter/>
                </div>
            </div>
            ) ;
    }
}

render(<App/>, document.getElementById('app'));