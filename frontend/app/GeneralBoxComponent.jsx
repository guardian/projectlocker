import React from 'react';
import axios from 'axios';
import SortableTable from 'react-sortable-table';
import {Link} from 'react-router-dom';
import moment from 'moment';

import ProjectBoxLeftComponent from './ProjectBoxLeftComponent.jsx'
import ProjectBoxMiddleComponent from './ProjectBoxMiddleComponent.jsx'
import ProjectBoxRightComponent from './ProjectBoxRightComponent.jsx'
import ProjectBoxMiddleComponentDiv from './ProjectBoxMiddleComponentDiv.jsx'

class GeneralBoxComponent extends React.Component {
    static ITEM_LIMIT=50;

    constructor(props){
        super(props);
        this.state = {
            data: [],
            hovered: false,
            filterTerms: {
                match: "W_ENDSWITH"
            },
            currentPage: 0,
            maximumItemsLoaded: false,
            plutoConfig: {},
            uid: "",
            isAdmin: false,
            interfaceSize: 0
        };

        this.pageSize = 20;

        this.gotDataCallback = this.gotDataCallback.bind(this);
        this.newElementCallback = this.newElementCallback.bind(this);
        this.filterDidUpdate = this.filterDidUpdate.bind(this);

        /* this must be supplied by a subclass */
        this.endpoint='/unknown';
        this.filterEndpoint = null;

        this.style = {
            backgroundColor: '#eee',
            border: '1px solid black',
            borderCollapse: 'collapse'
        };

        this.iconStyle = {
            color: '#aaa',
            paddingLeft: '5px',
            paddingRight: '5px'
        };

        this.canCreateNew=true;
        /* this must be supplied by a subclass */
        this.columns = [

        ];

        /*this.interfaceSize = 0;*/
    }

    componentDidMount() {
        this.loadDependencies().then(()=>{
            this.dependenciesDidLoad();
            this.reload();
        });
    }

    /**
     * override this in a subclass to update state once dependencies have loaded
     */
    dependenciesDidLoad(){

    }

    loadDependencies(){
        return new Promise((accept,reject)=>axios.get("/api/isLoggedIn")
            .then(response=>{
                if(response.data.status==="ok")
                    this.setState({isAdmin: response.data.isAdmin, uid: response.data.uid}, ()=>accept());
            })
            .catch(error=>{
                if(this.props.error.response && this.props.error.response.status===403)
                    this.setState({isAdmin: false}, ()=>accept());
                else {
                    console.error(error);
                    this.setState({isAdmin: false, error: error}, ()=>reject());
                }
            })
        );
    }

    /* this method supplies a column definition as a convenience for subclasses */
    static standardColumn(name, key) {
        return {
            header: name,
            key: key,
            headerProps: { className: 'dashboardheader'},
            render: (value)=><span style={{fontStyle: "italic"}}>n/a</span> ? value : (value && value.length>0)
        };
    }

    /* this method supplies a column definition for datetimes */
    static dateTimeColumn(name,key) {
        return {
            header: name,
            key: key,
            headerProps: {className: 'dashboardheader'},
            render: value=><span className="datetime">{moment(value).format("ddd Do MMM, HH:mm")}</span>
        }
    }

    breakdownPathComponents() {
        return this.props.location.pathname.split('/')
    }

    /* this method supplies the edit and delete icons. Can't be static as <Link> relies on the object context to access
     * history. */
    actionIcons() {
        const componentName = this.breakdownPathComponents()[1];
        return {
            header: "",
            key: "id",
            render: (id) => <span className="icons" style={{display: this.state.isAdmin ? "inherit" : "none"}}>
                    <Link to={"/" + componentName + "/" + id}><img className="smallicon" src="/assets/images/edit.png"/></Link>
                    <Link to={"/" + componentName + "/" + id + "/delete"}><img className="smallicon" src="/assets/images/delete.png"/></Link>
            </span>
        }
    }

    /* loads the next page of data */
    getNextPage(){
        const startAt = this.state.currentPage * this.pageSize;
        const length = this.pageSize;

        const axiosFuture = this.filterEndpoint ?
            axios.put(this.filterEndpoint + "?startAt=" + startAt + "&length=" + length, this.state.filterTerms) :
            axios.get(this.endpoint + "?startAt=" + startAt + "&length=" + length);

        axiosFuture.then(response=>{
            this.setState({
                currentPage: this.state.currentPage+1
            }, ()=>{
                this.gotDataCallback(response, ()=> {
                    if (response.data.result.length > 0)
                        if(this.pageSize*this.state.currentPage>=GeneralListComponent.ITEM_LIMIT)
                            this.setState({maximumItemsLoaded: true});
                        else
                            this.getNextPage()
                });
            })
        }).catch(error=>{
            console.error(error);
        });
    }

    /* reloads the data for the component based on the endpoint configured in the constructor */
    reload(){
        this.setState({
            currentPage: 0,
            data: []
        }, ()=> this.getNextPage());
    }

    /* called when we receive data; can be over-ridden by a subclass to do something more clever */
    gotDataCallback(response, cb){
        this.setState({
            data: this.state.data.concat(response.data.result)
        }, cb);
    }

    /* called when the New button is clicked; can be over-ridden by a subclass to do something more clever */
    newElementCallback(event){

    }

    /* called to insert a filtering component; should be over-ridden by a subclass if filtering is required */
    getFilterComponent(){
        return <span/>
    }

    /* this can be referenced from a filter component in a subclass and should be called to update the active filtering.
    this will cause a reload of data from the server
     */
    filterDidUpdate(newterms){
        this.setState({filterTerms: newterms},()=>this.reload());
    }

    itemLimitWarning(){
        if(this.state.maximumItemsLoaded)
            return <p className="warning-text"><i className="fa-info fa" style={{marginRight: "0.5em", color: "orange"}}/>Maximum of {GeneralBoxComponent.ITEM_LIMIT} items have been loaded. Use filters to narrow this down.</p>
        else
            return <p style={{margin: 0}}/>
    }

    getProjects(){
        var i;
        var code_to_return = [];
        for (i = 0; i < this.state.data.length; i++) {
            if (this.state.interfaceSize == 0) {
                code_to_return[i] = <div className="project_box_div_version_small"><ProjectBoxLeftComponent size={this.state.interfaceSize} /><ProjectBoxMiddleComponentDiv id={this.state.data[i]['id']} type={this.state.data[i]['projectTypeId']} title={this.state.data[i]['title']} user={this.state.data[i]['user']} size={this.state.interfaceSize} /><ProjectBoxRightComponent size={this.state.interfaceSize} /></div>;
            } else {
                code_to_return[i] = <div className="project_box_div_version"><ProjectBoxLeftComponent size={this.state.interfaceSize} /><ProjectBoxMiddleComponentDiv id={this.state.data[i]['id']} type={this.state.data[i]['projectTypeId']} title={this.state.data[i]['title']} user={this.state.data[i]['user']} size={this.state.interfaceSize} /><ProjectBoxRightComponent size={this.state.interfaceSize} /></div>;
            }
        }

        return code_to_return
    }

    changeSize(){
        if (this.state.interfaceSize == 0) {
            this.setState({interfaceSize: 1},()=>this.reload());
        } else {
            this.setState({interfaceSize: 0},()=>this.reload());
        }
    }

    render() {
        return (
            <div>
                <span className="list-title"><h2 className="list-title">{this.props.title}</h2></span>
                {this.getFilterComponent()}
                {this.itemLimitWarning()}

                <span className="banner-control">
                    <button id="newElementButton" onClick={this.newElementCallback}>New</button>
                    <button className="size_button" onClick={this.changeSize.bind(this)}>Change Interface Size</button>
                </span>
                {this.getProjects()}
            </div>
        );
    }
}

export default GeneralBoxComponent;