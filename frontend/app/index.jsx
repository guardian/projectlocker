import React from 'react';
import {render} from 'react-dom';
import StorageListComponent from './StorageComponent.jsx';
import DefaultComponent from './DefaultComponent.jsx';
import RootComponent from './RootComponent.jsx';
import ProjectTemplateIndex from './ProjectTemplateIndex.jsx';

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

        this.routings = [
            ['^/$',M_ROOT,A_UNKNOWN],
            ['^/list/storages$',M_STORAGES,A_LIST],
            ['^/edit/storage/(\d+)$',M_STORAGES, A_EDIT],
            ['^/new/storage$',M_STORAGES, A_EDIT],
            ['^/delete/storage/(\d+)$',M_STORAGES, A_DELETE],
            ['^/list/templates$',M_TEMPLATES, A_LIST]
        ];

        this.compiledRoutings = this.routings.map((entry)=>{
            return [new RegExp(entry[0]),entry[1],entry[2]]
        });
    }

    componentWillMount() {
        this.setState({'target': this.match() })
    }

    match() {
        console.log("Current url path: " + location.pathname);
        const target = this.compiledRoutings.filter((entry)=>{
            const result = entry[0].exec(location.pathname);
            console.debug("checking " + location.pathname + " against " + entry[0]);
            console.debug(result);
            return result!=null;
        });
        if(target.length==0) return [M_UNKNOWN, A_UNKNOWN];
        console.debug(target);
        return [target[0][1], target[0][2]];
    }

    render() {
        console.debug(this.state.target);

        switch(this.state.target[0]){
            case M_STORAGES:
                return(<StorageListComponent mode={this.state.target[1]}/>);
            case M_TEMPLATES:
                return(<ProjectTemplateIndex mode={this.state.target[1]}/>);
            case M_ROOT:
                return(<RootComponent mode={this.state.target[1]}/>);
            default:
                return(<DefaultComponent mode={this.state.target[1]}/>);
        }
    }
}

class App extends React.Component {
    render () {
        return(
            <div>
                <div id="leftmenu" className="leftmenu">
                    <ul>
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