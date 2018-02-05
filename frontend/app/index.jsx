import React from 'react';
import {render} from 'react-dom';
import {BrowserRouter, Link, Route, Switch} from 'react-router-dom';
import StorageListComponent from './StorageComponent.jsx';

import RootComponent from './RootComponent.jsx';
import ProjectTemplateIndex from './ProjectTemplateIndex.jsx';
import ProjectTypeMultistep from './multistep/ProjectTypeMultistep.jsx';
import FileEntryList from './FileEntryList.jsx';
import ProjectEntryList from './ProjectEntryList.jsx';
import ProjectTypeList from './ProjectTypeList.jsx';

import StorageMultistep from './multistep/StorageMultistep.jsx';
import StorageDeleteComponent from './delete/StorageDeleteComponent.jsx';

import TypeDeleteComponent from './delete/TypeDeleteComponent.jsx';

window.React = require('react');

class App extends React.Component {
    render () {
        return(
            <div>
                <div id="leftmenu" className="leftmenu">
                    <ul className="leftmenu">
                        <li><Link to="/storage/">Storages...</Link></li>
                        <li><Link to="/type/">Project Types...</Link></li>
                        <li><Link to="/template/">Project Templates...</Link></li>
                        <li><Link to="/project/">Projects...</Link></li>
                        <li><Link to="/file/">Files...</Link></li>
                    </ul>
                </div>
                <div id="mainbody" className="mainbody">
                    <Switch>
                        <Route path="/storage/:itemid/delete" component={StorageDeleteComponent}/>
                        <Route path="/storage/:itemid" component={StorageMultistep}/>
                        <Route path="/storage/" component={StorageListComponent}/>
                        <Route path="/template/:itemid" component={ProjectTemplateIndex}/>
                        <Route path="/template/" component={ProjectTemplateIndex}/>
                        <Route path="/file/:itemid" component={FileEntryList}/>
                        <Route path="/file/" component={FileEntryList}/>
                        <Route path="/type/:itemid/delete" component={TypeDeleteComponent}/>
                        <Route path="/type/:itemid" component={ProjectTypeMultistep}/>
                        <Route path="/type/" component={ProjectTypeList}/>
                        <Route path="/project/:itemid" component={ProjectEntryList}/>
                        <Route path="/project/" component={ProjectEntryList}/>
                        <Route exact path="/" component={RootComponent}/>
                    </Switch>
                </div>
            </div>
            ) ;
    }
}

render(<BrowserRouter root="/"><App/></BrowserRouter>, document.getElementById('app'));