import React from 'react';
import {render} from 'react-dom';
import {BrowserRouter, Link, Route, Switch} from 'react-router-dom';
import StorageListComponent from './StorageComponent.jsx';

import RootComponent from './RootComponent.jsx';

import ProjectTypeMultistep from './multistep/ProjectTypeMultistep.jsx';
import FileEntryList from './FileEntryList.jsx';
import ProjectEntryList from './ProjectEntryList.jsx';
import ProjectTypeList from './ProjectTypeList.jsx';

import StorageMultistep from './multistep/StorageMultistep.jsx';
import StorageDeleteComponent from './delete/StorageDeleteComponent.jsx';

import TypeDeleteComponent from './delete/TypeDeleteComponent.jsx';

import ProjectTemplateIndex from './ProjectTemplateIndex.jsx';
import ProjectTemplateMultistep from './multistep/ProjectTemplateMultistep.jsx';
import ProjectTemplateDeleteComponent from './delete/ProjectTemplateDeleteComponent.jsx';

import ProjectDeleteComponent from './delete/ProjectEntryDeleteComponent.jsx';

import ProjectCreateMultistep from './multistep/ProjectCreateMultistep.jsx';
import TitleEditComponent from './multistep/projectcreate/ProjectEntryEditComponent.jsx';

import PostrunList from './PostrunList.jsx';
import PostrunMultistep from './multistep/PostrunMultistep.jsx';
import PostrunDeleteComponent from './delete/PostrunDeleteComponent.jsx';

import axios from 'axios';

window.React = require('react');

class App extends React.Component {
    constructor(props){
        super(props);

        this.state = {
            isLoggedIn: false,
            currentUsername: ""
        };

        this.onLoggedIn = this.onLoggedIn.bind(this);
        this.onLoggedOut = this.onLoggedOut.bind(this);
    }

    checkLogin(){
        this.setState({loading: true, haveChecked: true}, ()=>
            axios.get("/api/isLoggedIn")
                .then(response=>{ //200 response means we are logged in
                    this.setState({
                        isLoggedIn: true,
                        currentUsername: response.data.uid
                    });
                })
                .catch(error=>{
                    this.setState({
                        isLoggedIn: false,
                        currentUsername: ""
                    })
                })
        );
    }

    componentWillMount(){
        this.checkLogin();
    }

    onLoggedIn(userid){
        console.log("Logged in as " + userid);
        this.setState({currentUsername: userid, isLoggedIn: true})
    }

    onLoggedOut(){
        this.setState({currentUsername: "", isLoggedIn: false})
    }

    /*show left menu if logged in*/
    maybeLeftMenu(){
        if(this.state.isLoggedIn) {
            return <ul className="leftmenu">
                <li><Link to="/storage/">Storages...</Link></li>
                <li><Link to="/type/">Project Types...</Link></li>
                <li><Link to="/template/">Project Templates...</Link></li>
                <li><Link to="/project/">Projects...</Link></li>
                <li><Link to="/postrun/">Postrun Actions...</Link></li>
                <li><Link to="/file/">Files...</Link></li>
            </ul>
        } else {
            return <ul className="leftmenu">
                <li><i>Not logged in</i></li>
            </ul>
        }
    }

    render () {
        return(
            <div>
                <div id="leftmenu" className="leftmenu">
                    {this.maybeLeftMenu()}
                </div>
                <div id="mainbody" className="mainbody">
                    <Switch>
                        <Route path="/storage/:itemid/delete" component={StorageDeleteComponent}/>
                        <Route path="/storage/:itemid" component={StorageMultistep}/>
                        <Route path="/storage/" component={StorageListComponent}/>
                        <Route path="/template/:itemid/delete" component={ProjectTemplateDeleteComponent}/>
                        <Route path="/template/:itemid" component={ProjectTemplateMultistep}/>
                        <Route path="/template/" component={ProjectTemplateIndex}/>
                        <Route path="/file/:itemid" component={FileEntryList}/>
                        <Route path="/file/" component={FileEntryList}/>
                        <Route path="/type/:itemid/delete" component={TypeDeleteComponent}/>
                        <Route path="/type/:itemid" component={ProjectTypeMultistep}/>
                        <Route path="/type/" component={ProjectTypeList}/>
                        <Route path="/project/new" component={ProjectCreateMultistep}/>
                        <Route path="/project/:itemid/delete" component={ProjectDeleteComponent}/>
                        <Route path="/project/:itemid" component={TitleEditComponent}/>
                        <Route path="/project/" component={ProjectEntryList}/>
                        <Route path="/postrun/:itemid/delete" component={PostrunDeleteComponent}/>
                        <Route path="/postrun/:itemid" component={PostrunMultistep}/>
                        <Route path="/postrun/" component={PostrunList}/>
                        <Route exact path="/" component={()=><RootComponent
                            onLoggedOut={this.onLoggedOut}
                            onLoggedIn={this.onLoggedIn}
                            currentUsername={this.state.currentUsername}
                            isLoggedIn={this.state.isLoggedIn}
                        />}/>
                    </Switch>
                </div>
            </div>
            ) ;
    }
}

render(<BrowserRouter root="/"><App/></BrowserRouter>, document.getElementById('app'));